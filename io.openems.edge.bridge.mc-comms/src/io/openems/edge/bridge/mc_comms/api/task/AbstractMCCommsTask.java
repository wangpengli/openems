package io.openems.edge.bridge.mc_comms.api.task;

import io.openems.edge.bridge.mc_comms.MCCommsProtocol;
import io.openems.edge.bridge.mc_comms.api.element.MCCommsElement;
import io.openems.edge.common.taskmanager.Priority;

public abstract class AbstractMCCommsTask implements MCCommsTask {

    protected final MCCommsElement<?>[] elements;
    protected final Priority priority;
    protected MCCommsProtocol protocol;
    private int destinationAddress;

    AbstractMCCommsTask(Priority priority, MCCommsElement<?>... elements) {
        this.priority = priority;
        this.elements = elements;
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
}
