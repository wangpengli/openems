package io.openems.edge.bridge.mc_comms.api.task;

import io.openems.edge.bridge.mc_comms.util.MCCommsProtocol;

public interface MCCommsTask {

    void setProtocol(MCCommsProtocol protocol);

    MCCommsProtocol getProtocol();
}
