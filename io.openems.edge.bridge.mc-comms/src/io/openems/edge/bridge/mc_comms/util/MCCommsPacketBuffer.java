package io.openems.edge.bridge.mc_comms.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.*;

public class MCCommsPacketBuffer {

    private LinkedBlockingDeque<TimedByte> timedByteQueue = new LinkedBlockingDeque<>();
    private ConcurrentSkipListMap<Long, MCCommsPacket> RXPacketQueue = new ConcurrentSkipListMap<>();
    public ConcurrentLinkedDeque<MCCommsPacket> TXPacketQueue = new ConcurrentLinkedDeque<>();
    private PacketBuilder packetBuilder;
    private ConcurrentHashMap<Integer, LinkedTransferQueue<MCCommsPacket>> transferQueues = new ConcurrentHashMap<>();
    private SerialByteHandler serialByteHandler;
    private PacketPicker packetPicker;

    public void start(InputStream serialInputStream, OutputStream serialOutputStream) {
        //byte reader
        this.serialByteHandler = new SerialByteHandler(serialInputStream, serialOutputStream, timedByteQueue);
        this.serialByteHandler.run();
        //packet builder
        this.packetBuilder = new PacketBuilder(timedByteQueue, this.RXPacketQueue);
        this.packetBuilder.run();
        //packet picker
        this.packetPicker = new PacketPicker();
        this.packetPicker.run();
    }

    public void stop() {
        this.serialByteHandler.destroy();
        this.packetBuilder.destroy();
        this.packetPicker.destroy();
    }

    public void registerTransferQueue(int sourceAddress, LinkedTransferQueue<MCCommsPacket> transferQueue) {
        transferQueues.put(sourceAddress, transferQueue);
    }

    private class PacketPicker extends Thread {
        public void run() {
            while (true) {
                try {
                    for (Map.Entry<Long, MCCommsPacket> entry : RXPacketQueue.entrySet()) {
                        MCCommsPacket entryPacket = entry.getValue();
                        TransferQueue<MCCommsPacket> correspondingQueue = transferQueues.get(entryPacket.sourceAddress);
                        if (correspondingQueue != null) {
                            correspondingQueue.tryTransfer(entryPacket, 5, TimeUnit.MILLISECONDS); //timeout on trying to give packets to packet handlers to prevent packet queue from filling up too fast
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
            TimedByte polledTimedByte;

            //forever loop
            while (true) {
                polledTimedByte = timedByteQueue.pollLast(); //blocking deque will block until a value is present in the deque PERFORMANCE GAINZ!!1!
                currentByte = polledTimedByte.getValue();
                currentByteTime = polledTimedByte.getTime();
                boolean endByteReceived = false;
                if (currentByte == 83) { //don't start constructing packets until start character 'S' is received
                    long packetStartTime = currentByteTime;
                    long byteTimeDelta = 0;
                    //byte consumer loop
                    while ((currentByteTime - packetStartTime) < 35000000L && packetBuffer.position() < 24) {
                        //if packet time window has not closed and packet byte buffer is not yet full
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
                        //get next timed-byte
                        polledTimedByte = timedByteQueue.pollLast();
                        //record byte rx time for new byte
                        currentByteTime = polledTimedByte.getTime();
                        //assign byte to holder var for next loop
                        currentByte = polledTimedByte.getValue();
                        //get time difference between current and last byte
                        byteTimeDelta = currentByteTime - previousByteTime;
                        if (currentByte == 69) {
                            endByteReceived = true; //test if packet has truly ended on next byte consumer loop
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
                    //reset buffer
                    packetBuffer.position(0);
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
                if (!TXPacketQueue.isEmpty())
                    TXPacketQueue.forEach(packet -> {
                        try {
                            outputStream.write(packet.getPacketBuffer().array());
                            outputStream.flush();
                        } catch (IOException e) {
                            resume(); //TODO log write errors
                        }
                    });
                try {
                    if (inputStream.available() > 0) {
                        timedByteQueue.add(new TimedByte(System.nanoTime(), (byte) inputStream.read()));
                    }
                    sleep(1); //prevent maxing out the CPU on serial IO reads - TODO check system resource usage, may be heavy on small systems
                } catch (IOException | InterruptedException ignored) {
                    resume(); //ignore exceptions and keep running
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
