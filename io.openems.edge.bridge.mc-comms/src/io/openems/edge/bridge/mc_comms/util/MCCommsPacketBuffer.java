package io.openems.edge.bridge.mc_comms.util;

import com.fazecast.jSerialComm.SerialPort;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.IntBuffer;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class MCCommsPacketBuffer {

    //serial port
    private AtomicReference<SerialPort> serialPortAtomicRef = new AtomicReference<>();
    //concurrent queues for threads
    private final LinkedBlockingDeque<TimedByte>
            timedByteQueue = new LinkedBlockingDeque<>();
    private final Cache<Long, MCCommsPacket>
            RXPacketCache = CacheBuilder.newBuilder()
                .expireAfterWrite(Duration.ofMillis(500))
                .maximumSize(1000)
                .build();
    private final ConcurrentLinkedDeque<MCCommsPacket>
            TXPacketQueue = new ConcurrentLinkedDeque<>();
    private final ConcurrentHashMap<Integer, LinkedTransferQueue<MCCommsPacket>>
            transferQueues = new ConcurrentHashMap<>();
    //thread executor
    private final Executor executor = Executors.newFixedThreadPool(3);
    //utility objects for byte/packet handling
    private final PacketBuilder packetBuilder = new PacketBuilder(timedByteQueue, this.RXPacketCache);
    private final SerialByteHandler serialByteHandler;
    private final PacketPicker packetPicker = new PacketPicker();

    public MCCommsPacketBuffer(String serialPortDescriptor) {
        this.serialPortAtomicRef.set(SerialPort.getCommPort(serialPortDescriptor));
        this.serialPortAtomicRef.get().setComPortParameters(9600, 8, 0, SerialPort.NO_PARITY);
        this.serialPortAtomicRef.get().setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
        this.serialPortAtomicRef.get().setComPortTimeouts(SerialPort.TIMEOUT_NONBLOCKING, 0, 0);
        this.serialByteHandler = new SerialByteHandler(this.serialPortAtomicRef, timedByteQueue);
    }

    public void start() {
        //open serial port
        this.serialPortAtomicRef.get().openPort();
        //execution
        //byte reader
        this.executor.execute(serialByteHandler);
        //packet builder
        this.executor.execute(packetBuilder);
        //packet picker
        this.executor.execute(packetPicker);
    }

    public void stop() {
        this.serialByteHandler.stop();
        this.packetBuilder.stop();
        this.packetPicker.stop();
        this.serialPortAtomicRef.get().closePort();
    }

    void registerTransferQueue(int sourceAddress, LinkedTransferQueue<MCCommsPacket> transferQueue) {
        transferQueues.put(sourceAddress, transferQueue);
    }

    public ConcurrentLinkedDeque<MCCommsPacket> getTXPacketQueue() {
        return TXPacketQueue;
    }

    private class PacketPicker extends Thread {
        public void run() {
            while (true) {
                //iterate through queued packets and try assign them to component transfer queues
                for (Map.Entry<Long, MCCommsPacket> entry : RXPacketCache.asMap().entrySet()) {
                    MCCommsPacket entryPacket = entry.getValue();
                    TransferQueue<MCCommsPacket> correspondingQueue = transferQueues.get(entryPacket.sourceAddress);
                    //if a corresponding queue exists...
                    if (correspondingQueue != null) {
                        //...try transfer to that queue
                        if (correspondingQueue.tryTransfer(entryPacket)) //if transferred...
                            RXPacketCache.invalidate(entry.getKey()); //...remove packet from cache
                    }
                }
                try {
                    synchronized (packetBuilder) {
                        packetBuilder.wait(); //waits on packet builder to actually produce packets
                    }
                } catch (InterruptedException e) {
                    packetBuilder.resume(); //resume packet builder if interrupted for some reason
                }
                RXPacketCache.cleanUp(); //clean up cache
            }
        }
    }

    private class PacketBuilder extends Thread {

        private LinkedBlockingDeque<TimedByte> timedByteQueue;
        private Cache<Long, MCCommsPacket> outputCache;

        private PacketBuilder(LinkedBlockingDeque<TimedByte> timedByteQueue, Cache<Long, MCCommsPacket> outputCache) {
            this.timedByteQueue = timedByteQueue;
            this.outputCache = outputCache;
        }

        @Override
        public void run() {
            ByteBuffer packetBuffer = ByteBuffer.allocate(25);
            byte currentByte;
            long currentByteTime;
            long previousByteTime;
            long packetStartTime;
            long byteTimeDelta;
            TimedByte polledTimedByte;

            //forever loop
            while (true) {
                //blocking deque will block until a value is present in the deque PERFORMANCE GAINZ!!1!
                try {
                    polledTimedByte = timedByteQueue.takeFirst();
                } catch (InterruptedException e) {
                    continue;
                }
                currentByte = polledTimedByte.getValue();
                currentByteTime = polledTimedByte.getTime();
                boolean endByteReceived = false;
                if (currentByte == 0x53) { //don't start constructing packets until start character 'S' is received
                    packetStartTime = currentByteTime;
                    previousByteTime = currentByteTime;
                    //byte consumer loop
                    while ((currentByteTime - packetStartTime) < 35000000L && packetBuffer.position() <= 24 && polledTimedByte != null) {
                        //while packet window period (35ms) has not closed and packet is not full
                        //get time difference between current and last byte
                        byteTimeDelta = currentByteTime - previousByteTime;
                        //put byte in buffer, record byte rx time
                        previousByteTime = currentByteTime;
                        packetBuffer.put(currentByte);
                        if (endByteReceived && (byteTimeDelta > 3000000L)){
                            //if endByte has been received and a pause of more than 3ms has elapsed...
                            break; //... break out of byte consumer loop
                        } else if(endByteReceived && packetBuffer.position() <= 24) {
                            endByteReceived = false; //if payload byte is coincidentally 'E', prevent packet truncation
                        }
                        //calculate time remaining in packet window
                        long remainingPacketWindowPeriod = 35000000L - (currentByteTime - packetStartTime);
                        //get next timed-byte
                        try {
                            // ...and time out polling operation if window closes
                            polledTimedByte = timedByteQueue.pollFirst(remainingPacketWindowPeriod, TimeUnit.NANOSECONDS);
                        } catch (InterruptedException ignored) {
                            continue;
                        }
                        if (polledTimedByte != null) {
                            //record byte rx time for new byte
                            currentByteTime = polledTimedByte.getTime();
                            //assign byte to holder var for next loop
                            currentByte = polledTimedByte.getValue();
                            if (currentByte == 69) {
                                endByteReceived = true; //test if packet has truly ended on next byte consumer loop
                            }
                        }
                    }
                    if (packetBuffer.position() > 24) { //if the packet is long enough
                        try {
                            outputCache.put(currentByteTime, new MCCommsPacket(packetBuffer.array()));
                            synchronized (this) {
                                this.notifyAll(); //notify waiting threads that packets are available
                            }
                        } catch (MCCommsException ignored) {
                            //malformed packet; ignored
                        }
                    }
                    //reset buffer position
                    packetBuffer.position(0);
                }
            }
        }
    }

    private class SerialByteHandler extends Thread {

        private final AtomicReference<SerialPort> serialPortAtomicRef;
        private InputStream inputStream;
        private OutputStream outputStream;
        private LinkedBlockingDeque<TimedByte> timedByteQueue;

        SerialByteHandler(AtomicReference<SerialPort> serialPortAtomicRef, LinkedBlockingDeque<TimedByte> timedByteQueue) {
            this.serialPortAtomicRef = serialPortAtomicRef;
            this.timedByteQueue = timedByteQueue;
        }

        @Override
        public void run() {
            while (true) {
                if (this.inputStream == null) {
                    this.inputStream = this.serialPortAtomicRef.get().getInputStream();
                    continue;
                }
                if (this.outputStream == null) {
                    this.outputStream = this.serialPortAtomicRef.get().getOutputStream();
                    continue;
                }
                try {
                    while (this.serialPortAtomicRef.get().isOpen() && inputStream.available() > 0) {
                        timedByteQueue.add(new TimedByte(System.nanoTime(), (byte) inputStream.read()));
                    }
                } catch (IOException ignored) {
                    resume(); //ignore exceptions and keep running
                }
                if (!getTXPacketQueue().isEmpty())
                    getTXPacketQueue().forEach(packet -> {
                        try {
                            outputStream.write(packet.getPacketBuffer().array());
                            outputStream.flush();
                        } catch (IOException e) {
                            resume(); //TODO log write errors
                        }
                    });
                try {
                    sleep(1); //prevent maxing out the CPU on serial IO reads - TODO check system resource usage, may be heavy on small systems
                } catch (InterruptedException ignored) {
                    resume();
                }
            }
        }
    }

    private class TimedByte{

        private final long time;
        private final byte value;

        TimedByte(long time, byte value) {
            this.time = time;
            this.value = value;
        }

        long getTime() {
            return time;
        }

        byte getValue() {
            return value;
        }
    }
}
