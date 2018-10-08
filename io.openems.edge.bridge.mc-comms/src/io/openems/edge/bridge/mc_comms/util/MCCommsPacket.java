package io.openems.edge.bridge.mc_comms.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

public class MCCommsPacket {
    protected int sourceAddress;
    protected int destinationAddress;
    protected int command;
    protected int[] payload = new int[15];
    private IntBuffer packetBuffer = IntBuffer.allocate(25);

    public MCCommsPacket(int[] packetBuffer) throws MCCommsException {
        //packet length
        if (packetBuffer.length > 25) {
            throw new MCCommsException("MCComms packet too long! packet byte count: " + packetBuffer.length);
        }
        if (packetBuffer.length < 25) {
            throw new MCCommsException("MCComms packet too short! packet byte count: " + packetBuffer.length);
        }

        //break down packet

        //start character
        if (packetBuffer[0] != 83) {
            throw new MCCommsException("Invalid start character; expecting 83 but got: '" + packetBuffer[0]);
        }

        //end character
        if (packetBuffer[24] != 69) {
            throw new MCCommsException("Invalid end character; expecting 69 but got: '" + packetBuffer[24]);
        }

        //CRC
        int actualCRC = 0;
        for (int i = 0; i < 22; i ++) {
            actualCRC += packetBuffer[i];
        }
        ByteBuffer CRCBytes = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN);

        CRCBytes.put(2, (byte) packetBuffer[22]);
        CRCBytes.put(3, (byte) packetBuffer[23]);
        int packetCRC = CRCBytes.getInt();
        if (packetCRC != actualCRC) {
            throw new MCCommsException("CRC error! expected CRC value " + packetCRC + " but calculated CRC value " + actualCRC);
        }

        //command
        this.command = packetBuffer[5];

        //slave address
        ByteBuffer sourceAddressBytes = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN);
        sourceAddressBytes.put(2, (byte) packetBuffer[3]);
        sourceAddressBytes.put(3, (byte) packetBuffer[4]);
        this.sourceAddress = sourceAddressBytes.getInt();

        //master address
        ByteBuffer destinationAddressBytes = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN);
        destinationAddressBytes.put(2, (byte) packetBuffer[1]);
        destinationAddressBytes.put(3, (byte) packetBuffer[2]);
        this.destinationAddress = destinationAddressBytes.getInt();

        //payload
        System.arraycopy(packetBuffer, 7, this.payload, 0, 15); // only copy the payload
    }


    public MCCommsPacket(int command, int sourceAddress, int destinationAddress, byte[] payload) throws MCCommsException {
        if (payload.length != 15)
            throw new MCCommsException("[MCCOMMS] payload too short! expected payload byte length 15 but found byte length " + payload.length);
        this.command = command;
        this.sourceAddress = sourceAddress;
        this.destinationAddress = destinationAddress;
        for (int i = 0; i <= 15; i++) {
            this.payload[i] = payload[i];
        }
        this.createPacketBuffer();
    }

    public MCCommsPacket(int command, int sourceAddress, int destinationAddress) {
        this.command = command;
        this.sourceAddress = sourceAddress;
        this.destinationAddress = destinationAddress;
        for (int i = 0; i <= 15; i++) {
            this.payload[i] = 0xAA;
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
        this.packetBuffer.put(this.destinationAddress);
        this.packetBuffer.put(this.sourceAddress);
        this.packetBuffer.put((byte) this.command); //
        this.packetBuffer.put(this.payload);
        int CRC = 0;
        for (int i = 1; i <= 19; i++) {
            CRC += this.packetBuffer.get(i);
        }
        this.packetBuffer.position(20);
        this.packetBuffer.put(CRC);
        this.packetBuffer.put(69); //E end byte
    }

    public ByteBuffer getPacketBuffer() {
        ByteBuffer packetByteBuffer = ByteBuffer.allocate(25);
        //start char
        packetByteBuffer.put(ByteBuffer.allocate(4).putInt(packetBuffer.get(0)).array()[3]);
        //destination
        packetByteBuffer.put(ByteBuffer.allocate(4).putInt(packetBuffer.get(1)).array()[2]);
        packetByteBuffer.put(ByteBuffer.allocate(4).putInt(packetBuffer.get(1)).array()[3]);
        //source
        packetByteBuffer.put(ByteBuffer.allocate(4).putInt(packetBuffer.get(2)).array()[2]);
        packetByteBuffer.put(ByteBuffer.allocate(4).putInt(packetBuffer.get(2)).array()[3]);
        //command
        packetByteBuffer.put(ByteBuffer.allocate(4).putInt(packetBuffer.get(3)).array()[3]);
        //packet length
        packetByteBuffer.put(ByteBuffer.allocate(4).putInt(packetBuffer.get(4)).array()[3]);
        //payload
        for (int i = 5; i < 19; i++) {
            packetByteBuffer.put(ByteBuffer.allocate(4).putInt(packetBuffer.get(i)).array()[3]);
        }
        //crc
        packetByteBuffer.put(ByteBuffer.allocate(4).putInt(packetBuffer.get(20)).array()[2]);
        packetByteBuffer.put(ByteBuffer.allocate(4).putInt(packetBuffer.get(20)).array()[3]);
        //end char
        packetByteBuffer.put(ByteBuffer.allocate(4).putInt(packetBuffer.get(21)).array()[3]);
        return packetByteBuffer;
    }

    public int[] getPayload() {
        return payload;
    }
}
