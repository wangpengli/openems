package io.openems.edge.bridge.mc_comms.util;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import io.openems.edge.bridge.mc_comms.MCCommsBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class MCCommsPacketHandler implements SerialPortDataListener {

    private final Logger logger = LoggerFactory.getLogger(MCCommsPacketHandler.class);
    private int sourceAddress;
    private int destinationAddress;
    private Consumer<MCCommsRXPacket> onReadCallback;
    private MCCommsRXPacket packet;
    private InputStream inputStream;
    private  ByteBuffer packetBuffer = ByteBuffer.allocate(25);
    private int bufferPos = 1;
    private boolean endByteReceived = false;
    private Thread task = new Thread() {
        @Override
        public void run(){
            this.suspend();

        }
    };
    private ExecutorService executor = Executors.newCachedThreadPool();

    public void writePacket(MCCommsBridge bridge, MCCommsTXPacket txPacket) throws MCCommsException {
        Callable writeTask = () -> {
            bridge.getSerialPort().getOutputStream().write(txPacket.getPacketBuffer().array());

            return null;
        };
        Future future = executor.submit(writeTask);
        try {
            future.get(300, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new MCCommsException(e.getMessage());
        } finally {
            future.cancel(true);
        }
    }


    public MCCommsRXPacket readReply(int sourceAddress, MCCommsBridge bridge) throws MCCommsException {
        this.sourceAddress = sourceAddress;
        this.destinationAddress = bridge.getMasterAddress();
        this.inputStream = bridge.getSerialPort().getInputStream();
        bridge.getSerialPort().addDataListener(this);
        Future future = this.executor.submit(this.task);
        try {
            future.get(300, TimeUnit.MILLISECONDS);
            return this.packet;
        } catch (Exception e) {
            throw new MCCommsException(e.getMessage());
        } finally {
            future.cancel(true);
        }
    }

    @Override
    public int getListeningEvents() {
        return SerialPort.LISTENING_EVENT_DATA_RECEIVED;
    }

    @Override
    public void serialEvent(SerialPortEvent serialPortEvent) {
        try {
            int currByte = inputStream.read();
            if (currByte == 83) {
                long packetStartTime = System.nanoTime();
                long byteStartTime, byteNanoSecs = 0;
                try {
                    while ((System.nanoTime() - packetStartTime) < 35000000L && bufferPos < 25) {
                        if (endByteReceived && (byteNanoSecs > 3000000L)){
                            break;
                        } else {
                            endByteReceived = false;
                        }
                        byteStartTime = System.nanoTime();
                        currByte = inputStream.read();
                        byteNanoSecs = System.nanoTime() - byteStartTime;
                        packetBuffer.put(bufferPos++, (byte) currByte);
                        if (currByte == 69) {
                            endByteReceived = true;
                        }
                    }
                } catch (IOException e) {
                    logger.debug(e.getMessage());
                }
                packetBuffer.put(0, (byte) currByte);
            }
        } catch (IOException e) {
            logger.debug(e.getMessage());
        }
        if (endByteReceived && (bufferPos != 25)) { //if the endbyte timeout has broken the loop and the packet is too short
            logger.debug("End character timeout - incomplete packet");
            bufferPos = 1;
            endByteReceived = false;
            return;
        }
        if (bufferPos != 25) { //if the packet transmission window has closed and the packet is too short
            logger.debug("Packet timeout - incomplete packet");
            bufferPos = 1;
            endByteReceived = false;
            return;
        }
        try {
            MCCommsRXPacket rxPacket = new MCCommsRXPacket(packetBuffer.array());
            if ((rxPacket.getSourceAddress() == this.sourceAddress) &&
                    (rxPacket.getDestinationAddress() == this.destinationAddress)) {
                this.packet = rxPacket;
                this.task.resume();
            }
        } catch (MCCommsException e) {
            logger.debug(e.getMessage());
        }

    }
}
