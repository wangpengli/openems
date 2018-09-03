package io.openems.edge.bridge.mc_comms.api.element;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;

import java.util.Optional;

public interface MCCommsElement<T> {

    public int getLength();

    public void setMCCommsTask();

    public OpenemsType getType();

    public void _setNextWriteValue(Optional<T> valueOpt) throws OpenemsException;

}
