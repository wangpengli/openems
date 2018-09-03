package io.openems.edge.bridge.mc_comms.api.task;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.mc_comms.BridgeMCComms;
import io.openems.edge.common.taskmanager.ManagedTask;

public interface ReadTask extends Task, ManagedTask {

    /**
     * Sends a query for this AbstractTask to the MCComms device
     *
     * @param bridge
     * @throws OpenemsException
     */
    public abstract <T> void executeQuery(BridgeMCComms bridge) throws OpenemsException;
}
