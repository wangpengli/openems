package io.openems.edge.bridge.mc_comms;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortPacketListener;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.function.Consumer;

public class MCCommsPacketListener implements SerialPortPacketListener {

    private final int sourceAddress;
    private final int destinationAddress;
    private Consumer<byte[]> onReadCallback;

    public MCCommsPacketListener(int sourceAddress, BridgeMCComms bridge) {
        this.sourceAddress = sourceAddress;
        this.destinationAddress = bridge.getMasterAddress();
        bridge.getSerialPort().addDataListener(this);
    }

    public void addOnReadCallback(Consumer<byte[]> onReadCallback) {
        this.onReadCallback = onReadCallback;
    }

    @Override
    public int getListeningEvents() {
        return SerialPort.LISTENING_EVENT_DATA_RECEIVED;
    }

    @Override
    public void serialEvent(SerialPortEvent serialPortEvent) {
        try {
            MCCommsRXPacket rxPacket = new MCCommsRXPacket(serialPortEvent.getReceivedData());
            if ((rxPacket.getSourceAddress() == this.sourceAddress) &&
                    (rxPacket.getDestinationAddress() == this.destinationAddress)) {
                this.onReadCallback.accept(rxPacket.getPayload());
            }
        } catch (MCCommsException e) {
            //TODO
        }

    }

    @Override
    public int getPacketSize() {
        return 25;
    }

    private static class MCCommsRXPacket extends MCCommsPacket {

        MCCommsRXPacket (byte[] packet) throws MCCommsException {
            this.payload = new byte[15];
            //packet length
            if (packet.length > 25) {
                throw new MCCommsException("MCComms packet too long! packet byte count: " + packet.length);
            }
            if (packet.length < 25) {
                throw new MCCommsException("MCComms packet too short! packet byte count: " + packet.length);
            }

            //break down packet

            //start character
            ByteBuffer startCharByte = ByteBuffer.allocate(1);
            startCharByte.put(packet[0]);
            char startChar = startCharByte.getChar();
            if (startChar != 'S') {
                throw new MCCommsException("Invalid start character; expecting 'S' but got: '" + startChar + "' [" + startCharByte.toString() + "]");
            }

            //end character
            ByteBuffer endCharByte = ByteBuffer.allocate(1);
            endCharByte.put(packet[24]);
            char endChar = startCharByte.getChar();
            if (endChar != 'E') {
                throw new MCCommsException("Invalid end character; expecting 'E' but got: '" + endChar + "' [" + endCharByte.toString() + "]");
            }

            //CRC
            int actualCRC = 0;
            for (int i = 1; i < 21; i ++) {
                actualCRC += (int) packet[i];
            }
            ByteBuffer CRCBytes = ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN);
            CRCBytes.put(packet[3]);
            CRCBytes.put(packet[4]);
            int packetCRC = CRCBytes.getInt();
            if (packetCRC != actualCRC) {
                throw new MCCommsException("CRC error! expected CRC value " + packetCRC + " but calculated CRC value " + actualCRC);
            }

            //command
            ByteBuffer commandByte = ByteBuffer.allocate(1);
            commandByte.put(packet[5]);
            this.command = commandByte.getInt();

            //slave address
            ByteBuffer sourceAddressBytes = ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN);
            sourceAddressBytes.put(packet[3]);
            sourceAddressBytes.put(packet[4]);
            this.sourceAddress = sourceAddressBytes.getInt();

            //master address
            ByteBuffer destinationAddressBytes = ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN);
            destinationAddressBytes.put(packet[3]);
            destinationAddressBytes.put(packet[4]);
            this.destinationAddress = destinationAddressBytes.getInt();

            //payload
            System.arraycopy(packet, 7, this.payload, 0, 15); // only copy the payload
        }

        public byte[] getPayload() {
            return payload;
        }
    }
}
