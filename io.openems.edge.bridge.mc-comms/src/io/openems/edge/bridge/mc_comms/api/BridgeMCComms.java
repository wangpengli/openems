package io.openems.edge.bridge.mc_comms.api;

import io.openems.edge.bridge.mc_comms.util.MCCommsProtocol;
import io.openems.edge.common.component.OpenemsComponent;
import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface BridgeMCComms extends OpenemsComponent {

    void addProtocol(String sourceId, MCCommsProtocol protocol);

    void removeProtocol(String sourceId);
}
