package io.openems.edge.bridge.mc_comms.api.task;

import io.openems.edge.bridge.mc_comms.api.element.MCCommsElement;

public interface Task {
    void setSlaveAddress(int slaveAddress);

    MCCommsElement<?>[] getElements();
}
