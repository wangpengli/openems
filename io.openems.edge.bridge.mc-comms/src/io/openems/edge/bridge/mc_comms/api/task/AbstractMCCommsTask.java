package io.openems.edge.bridge.mc_comms.api.task;

import io.openems.edge.bridge.mc_comms.MCCommsBridge;
import io.openems.edge.bridge.mc_comms.util.*;
import io.openems.edge.bridge.mc_comms.api.element.MCCommsElement;
import io.openems.edge.common.taskmanager.Priority;

import java.util.function.Consumer;

public abstract class AbstractMCCommsTask implements MCCommsTask {

    protected final MCCommsElement<?>[] elements;
    protected final Priority priority;
    protected final int command;
    protected MCCommsProtocol protocol;
    private int destinationAddress;
    protected MCCommsPacketListener listener;
    protected MCCommsPacketSender sender;

    AbstractMCCommsTask(int command, Priority priority, MCCommsElement<?>... elements) {
        this.priority = priority;
        this.elements = elements;
        this.command = command;
    }

    @Override
    public void setProtocol(MCCommsProtocol protocol) {
        this.protocol = protocol;
    }

    @Override
    public MCCommsProtocol getProtocol() {
        return this.protocol;
    }

    public void setDestinationAddress(int address) {
        this.destinationAddress = address;
    }

    public int getDestinationAddress() {
        return this.destinationAddress;
    }

    public MCCommsElement<?>[] getElements() {
        return this.elements;
    }

    public Priority getPriority() {
        return priority;
    }

    public abstract void executeQuery(MCCommsBridge bridge) throws MCCommsException;

    protected void sendCommandPacket(MCCommsBridge bridge, int command, Consumer<MCCommsRXPacket> callback) throws MCCommsException {
        int slaveAddress = this.getProtocol().getSlaveAddress();
        this.listener = new MCCommsPacketListener(slaveAddress, bridge);
        this.sender = new MCCommsPacketSender(bridge);
        this.listener.addOnReadCallback(callback);
        try {
            this.sender.writePacket(new MCCommsTXPacket(command, bridge.getMasterAddress(), slaveAddress));
        } catch (MCCommsException e) {
            throw e;
        }

    }
}
