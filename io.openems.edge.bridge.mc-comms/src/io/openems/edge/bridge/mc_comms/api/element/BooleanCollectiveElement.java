package io.openems.edge.bridge.mc_comms.api.element;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;

import java.util.Optional;

public class BooleanCollectiveElement extends Abstract8BitElement<Byte> {

    BooleanCollectiveElement(int byteAddress) {
        super(byteAddress, OpenemsType.INTEGER);
    }

    @Override
    public Byte getValue() {
        return this.rawValue[0];
    }

    @Override
    public void setValue(Byte value) {
        this.rawValue[0] = value;
    }

    @Override
    public void setNextWriteValue(Optional<Byte> valueOpt) throws OpenemsException {
    }
}
