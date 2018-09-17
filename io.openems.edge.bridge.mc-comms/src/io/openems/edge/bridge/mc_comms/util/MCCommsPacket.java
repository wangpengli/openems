package io.openems.edge.bridge.mc_comms.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MCCommsPacket {
    protected int sourceAddress;
    protected int destinationAddress;
    protected int command;
    protected byte[] payload;
    private ByteBuffer packetBuffer = ByteBuffer.allocate(25);

    public MCCommsPacket(byte[] packet) throws MCCommsException {
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


    public MCCommsPacket(int command, int sourceAddress, int destinationAddress, byte[] payload) throws MCCommsException {
        if (payload.length != 15)
            throw new MCCommsException("[MCCOMMS] payload too short! expected payload byte length 15 but found byte length " + payload.length);
        this.command = command;
        this.sourceAddress = sourceAddress;
        this.destinationAddress = destinationAddress;
        this.payload = payload;
        this.createPacketBuffer();
    }

    public MCCommsPacket(int command, int sourceAddress, int destinationAddress) {
        this.command = command;
        this.sourceAddress = sourceAddress;
        this.destinationAddress = destinationAddress;
        for (int i = 0; i < 15; i++) {
            this.payload[i] = (byte) 0xAA;
        }
        this.createPacketBuffer();
    }


    public int getSourceAddress() {
        return sourceAddress;
    }

    public int getDestinationAddress() {
        return destinationAddress;
    }

    public int getCommand() {
        return command;
    }

    protected void createPacketBuffer() {
        this.packetBuffer.put((byte) 83); //S start byte
        this.packetBuffer.put(ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN).putInt(this.destinationAddress).array()); //destination address
        this.packetBuffer.put(ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN).putInt(this.sourceAddress).array()); //source address
        this.packetBuffer.put((byte) this.command); //
        this.packetBuffer.put(this.payload);
        int CRC = 0;
        for (int i = 1; i < 21; i++) {
            CRC += (int) this.packetBuffer.get(i);
        }
        this.packetBuffer.position(22);
        this.packetBuffer.put(ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN).putInt(CRC).array());
        this.packetBuffer.put((byte) 69); //E end byte
    }

    ByteBuffer getPacketBuffer() {
        return packetBuffer;
    }

    public byte[] getPayload() {
        return payload;
    }
}
