package io.openems.edge.bridge.mc_comms.api.task;

import io.openems.edge.bridge.mc_comms.MCCommsBridge;
import io.openems.edge.bridge.mc_comms.util.*;
import io.openems.edge.bridge.mc_comms.api.element.MCCommsElement;
import io.openems.edge.common.taskmanager.Priority;

public abstract class AbstractMCCommsTask implements MCCommsTask {

    final MCCommsElement<?>[] elements;
    private final Priority priority;
    final int command;
    private MCCommsProtocol protocol;
    MCCommsPacketHandler packetHandler;
    protected MCCommsBridge bridge;
    int slaveAddress;


    AbstractMCCommsTask(MCCommsBridge bridge, int slaveAddress, int command, Priority priority, MCCommsElement<?>... elements) {
        this.bridge = bridge;
        this.slaveAddress = slaveAddress;
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

    public Priority getPriority() {
        return priority;
    }

    public abstract void executeQuery() throws MCCommsException;
}
