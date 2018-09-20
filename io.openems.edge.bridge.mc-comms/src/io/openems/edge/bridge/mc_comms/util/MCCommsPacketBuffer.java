package io.openems.edge.bridge.mc_comms.util;

import com.fazecast.jSerialComm.SerialPort;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.*;

public class MCCommsPacketBuffer {

    //serial port
    private SerialPort serialPort;
    //concurrent queues for threads
    private final LinkedBlockingDeque<TimedByte>
            timedByteQueue = new LinkedBlockingDeque<>();
    private final ConcurrentSkipListMap<Long, MCCommsPacket>
            RXPacketQueue = new ConcurrentSkipListMap<>();
    private final ConcurrentLinkedDeque<MCCommsPacket>
            TXPacketQueue = new ConcurrentLinkedDeque<>();
    private final ConcurrentHashMap<Integer, LinkedTransferQueue<MCCommsPacket>>
            transferQueues = new ConcurrentHashMap<>();
    //thread executor
    private final Executor executor = Executors.newFixedThreadPool(3);
    //utility objects for byte/packet handling
    private PacketBuilder packetBuilder;
    private SerialByteHandler serialByteHandler;
    private PacketPicker packetPicker;

    public void start(String serialPortDescriptor) {
        //serial port
        this.serialPort = SerialPort.getCommPort(serialPortDescriptor);
        this.serialPort.setComPortParameters(9600, 8, 0, SerialPort.NO_PARITY);
        this.serialPort.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
        this.serialPort.setComPortTimeouts(SerialPort.TIMEOUT_NONBLOCKING, 0, 0);
        this.serialPort.openPort();
        //byte reader
        this.serialByteHandler = new SerialByteHandler(serialPort.getInputStream(), serialPort.getOutputStream(), timedByteQueue);
        this.executor.execute(serialByteHandler);
        //packet builder
        this.packetBuilder = new PacketBuilder(timedByteQueue, this.RXPacketQueue);
        this.executor.execute(packetBuilder);
        //packet picker
        this.packetPicker = new PacketPicker();
        this.executor.execute(packetPicker);
    }

    public void stop() {
        this.serialByteHandler.stop();
        this.packetBuilder.stop();
        this.packetPicker.stop();
        this.serialPort.closePort();
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
                try {
                    for (Map.Entry<Long, MCCommsPacket> entry : RXPacketQueue.entrySet()) {
                        MCCommsPacket entryPacket = entry.getValue();
                        TransferQueue<MCCommsPacket> correspondingQueue = transferQueues.get(entryPacket.sourceAddress);
                        if (correspondingQueue != null) {
                            //timeout on trying to give packets to tasks to prevent packet queue from filling up too fast
                            correspondingQueue.tryTransfer(entryPacket, 5, TimeUnit.MILLISECONDS);
                        }
                    }
                    packetBuilder.wait(); //waits on packet builder to actually produce packets
                } catch (InterruptedException ignored) {
                    packetBuilder.resume();
                }
            }
        }
    }

    private class PacketBuilder extends Thread {

        private LinkedBlockingDeque<TimedByte> timedByteQueue;
        private ConcurrentSkipListMap<Long, MCCommsPacket> outputSkipList;

        private PacketBuilder(LinkedBlockingDeque<TimedByte> timedByteQueue, ConcurrentSkipListMap<Long, MCCommsPacket> outputSkipList) {
            this.timedByteQueue = timedByteQueue;
            this.outputSkipList = outputSkipList;
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
                polledTimedByte = timedByteQueue.pollLast();
                if (polledTimedByte != null) {
                    currentByte = polledTimedByte.getValue();
                    currentByteTime = polledTimedByte.getTime();
                    boolean endByteReceived = false;
                    if (currentByte == 83) { //don't start constructing packets until start character 'S' is received
                        packetStartTime = currentByteTime;
                        previousByteTime = currentByteTime;
                        //byte consumer loop
                        while ((currentByteTime - packetStartTime) < 35000000L && packetBuffer.position() < 24) {
                            //while packet window period (35ms) has not closed and packet byte buffer is not yet full
                            //get time difference between current and last byte
                            byteTimeDelta = currentByteTime - previousByteTime;

                            if (endByteReceived && (byteTimeDelta > 3000000L)){
                                //if endByte has been received and a pause of more than 3ms has elapsed...
                                packetBuffer.put(currentByte); //... add endByte to buffer
                                break; //... and break out of byte consumer loop
                            } else {
                                endByteReceived = false; //if payload byte is coincidentally 'E', prevent packet truncation
                            }
                            //put byte in buffer, record byte rx time
                            previousByteTime = currentByteTime;
                            packetBuffer.put(currentByte);
                            //calculate time remaining in packet window
                            long remainingPacketWindowPeriod = 35000000L - (currentByteTime - packetStartTime);
                            //get next timed-byte
                            try {
                                // ...and time out polling operation if window closes
                                polledTimedByte = timedByteQueue.pollLast(remainingPacketWindowPeriod, TimeUnit.NANOSECONDS);
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
                        if (packetBuffer.position() == 24) { //if the packet is long enough
                            try {
                                //try put a new packet in the output queue
                                outputSkipList.put(currentByteTime, new MCCommsPacket(packetBuffer.array()));
                                notifyAll(); //notify waiting threads that packets are available
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
    }

    private class SerialByteHandler extends Thread {

        private InputStream inputStream;
        private OutputStream outputStream;
        private LinkedBlockingDeque<TimedByte> timedByteQueue;

        SerialByteHandler(InputStream inputStream, OutputStream outputStream, LinkedBlockingDeque<TimedByte> timedByteQueue) {
            this.inputStream = inputStream;
            this.outputStream = outputStream;
            this.timedByteQueue = timedByteQueue;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    while (inputStream.available() > 0) {
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
