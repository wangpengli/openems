package io.openems.edge.bridge.mc_comms.api.task;

import io.openems.edge.bridge.mc_comms.api.element.MCCommsElement;
import io.openems.edge.bridge.mc_comms.util.MCCommsException;
import io.openems.edge.bridge.mc_comms.util.MCCommsProtocol;
import io.openems.edge.common.taskmanager.Priority;

public abstract class AbstractMCCommsTask {

    final MCCommsElement<?>[] elements;
    private final Priority priority;
    final int command;
    private MCCommsProtocol protocol;


    AbstractMCCommsTask(int command, Priority priority, MCCommsElement<?>... elements) {
        this.priority = priority;
        this.elements = elements;
        this.command = command;
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
