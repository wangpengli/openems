package io.openems.edge.bridge.mc_comms.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MCCommsTXPacket extends MCCommsPacket {

    private ByteBuffer packetBuffer = ByteBuffer.allocate(25);

    public MCCommsTXPacket(int command, int sourceAddress, int destinationAddress, byte[] payload) {
        this.command = command;
        this.sourceAddress = sourceAddress;
        this.destinationAddress = destinationAddress;
        this.payload = payload;
        this.createPacketBuffer();
    }

    public MCCommsTXPacket(int command, int sourceAddress, int destinationAddress) {
        this.command = command;
        this.sourceAddress = sourceAddress;
        this.destinationAddress = destinationAddress;
        for (int i = 7; i < 21; i++) {
            this.payload[i] = (byte) 0xAA;
        }
        this.createPacketBuffer();
    }


    private void createPacketBuffer() {
        this.packetBuffer.put((byte) 83); //S start byte
        this.packetBuffer.put(ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN).putInt(this.destinationAddress).array());
        this.packetBuffer.put(ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN).putInt(this.sourceAddress).array());
        this.packetBuffer.put((byte) this.command);
        this.packetBuffer.put(this.payload);
        int CRC = 0;
        for (int i = 1; i < 21; i++) {
            CRC += (int) this.packetBuffer.get(i);
        }
        this.packetBuffer.position(22);
        this.packetBuffer.put(ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN).putInt(CRC).array());
        this.packetBuffer.put((byte) 69); //E end byte
    }

    public ByteBuffer getPacketBuffer() {
        return packetBuffer;
    }
}
