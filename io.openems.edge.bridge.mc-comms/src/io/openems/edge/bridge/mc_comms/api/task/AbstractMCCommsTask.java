package io.openems.edge.bridge.mc_comms.api.task;

import io.openems.edge.bridge.mc_comms.MCCommsBridge;
import io.openems.edge.bridge.mc_comms.util.*;
import io.openems.edge.bridge.mc_comms.api.element.MCCommsElement;
import io.openems.edge.common.taskmanager.Priority;

import java.util.concurrent.LinkedTransferQueue;

public abstract class AbstractMCCommsTask {

    final MCCommsElement<?>[] elements;
    private final Priority priority;
    final int command;
    private MCCommsProtocol protocol;
    protected MCCommsBridge bridge;
    int slaveAddress;
    LinkedTransferQueue<MCCommsPacket> transferQueue = new LinkedTransferQueue<>();


    AbstractMCCommsTask(MCCommsBridge bridge, int slaveAddress, int command, Priority priority, MCCommsElement<?>... elements) {
        this.bridge = bridge;
        this.slaveAddress = slaveAddress;
        this.priority = priority;
        this.elements = elements;
        this.command = command;
        bridge.IOPacketBuffer.registerTransferQueue(slaveAddress, transferQueue);
    }

    public void setProtocol(MCCommsProtocol protocol) {
        this.protocol = protocol;
    }

    public MCCommsProtocol getProtocol() {
        return this.protocol;
    }

    public Priority getPriority() {
        return priority;
    }

    public abstract void executeQuery() throws MCCommsException;
}
