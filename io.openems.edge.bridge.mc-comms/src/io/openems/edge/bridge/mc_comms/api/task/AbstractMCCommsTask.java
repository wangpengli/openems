package io.openems.edge.bridge.mc_comms.api.task;

import io.openems.edge.bridge.mc_comms.api.element.MCCommsElement;
import io.openems.edge.bridge.mc_comms.util.MCCommsException;
import io.openems.edge.bridge.mc_comms.util.MCCommsProtocol;
import io.openems.edge.common.taskmanager.Priority;

/**
 * Abstract class; parent of all MCComms Tasks
 */
public abstract class AbstractMCCommsTask {

    final MCCommsElement<?>[] elements;
    private final Priority priority;
    final int command;
    private MCCommsProtocol protocol;

    /**
     * Constructor
     * @param command command to be used
     * @param priority priority of this task
     * @param elements MCCommsElements belonging to this task
     */
    AbstractMCCommsTask(int command, Priority priority, MCCommsElement<?>... elements) {
        this.priority = priority;
        this.elements = elements;
        this.command = command;
    }

    /**
     * @param protocol the protocol this task belongs to
     */
    public void setProtocol(MCCommsProtocol protocol) {
        this.protocol = protocol;
    }

    /**
     * @return the protocol this task belongs to
     */
    public MCCommsProtocol getProtocol() {
        return this.protocol;
    }

    /**
     * @return the priority of this task
     */
    public Priority getPriority() {
        return priority;
    }

    /**
     * executes the read or write query
     * @throws MCCommsException generally IO exceptions
     */
    public abstract void executeQuery() throws MCCommsException;
}
