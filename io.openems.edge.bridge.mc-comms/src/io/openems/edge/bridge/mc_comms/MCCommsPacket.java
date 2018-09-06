package io.openems.edge.bridge.mc_comms;

public abstract class MCCommsPacket {
    protected int sourceAddress;
    protected int destinationAddress;
    protected int command;
    protected byte[] payload;

    public int getSourceAddress() {
        return sourceAddress;
    }

    public int getDestinationAddress() {
        return destinationAddress;
    }

    public int getCommand() {
        return command;
    }

}
