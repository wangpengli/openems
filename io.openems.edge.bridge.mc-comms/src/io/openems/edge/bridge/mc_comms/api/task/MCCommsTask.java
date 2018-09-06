package io.openems.edge.bridge.mc_comms.api.task;

import io.openems.edge.bridge.mc_comms.MCCommsProtocol;
import io.openems.edge.bridge.mc_comms.api.element.MCCommsElement;

public interface MCCommsTask {

    int getDestinationAddress();

    void setDestinationAddress(int address);

    MCCommsElement<?>[] getElements();

    void setProtocol(MCCommsProtocol protocol);

    MCCommsProtocol getProtocol();
}
