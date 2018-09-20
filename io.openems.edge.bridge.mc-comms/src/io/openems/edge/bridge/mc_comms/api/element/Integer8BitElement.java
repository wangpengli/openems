package io.openems.edge.bridge.mc_comms.api.element;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;

import java.nio.ByteBuffer;
import java.util.Optional;

public class Integer8BitElement extends Abstract8BitElement<Integer> {
    public Integer8BitElement(int byteAddress) {
        super(byteAddress, OpenemsType.INTEGER);
    }

    @Override
    public Integer getValue() {
        return (int) this.rawValue[0];
    }

    @Override
    public void setValue(Integer value) {
        this.rawValue = ByteBuffer.allocate(1).putInt(value).array();
    }

    @Override
    public void setNextWriteValue(Optional<Integer> valueOpt) throws OpenemsException {

    }
}
