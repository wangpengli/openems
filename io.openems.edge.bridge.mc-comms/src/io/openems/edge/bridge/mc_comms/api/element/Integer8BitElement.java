package io.openems.edge.bridge.mc_comms.api.element;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Optional;

public class Integer8BitElement extends Abstract8BitElement<Integer> {
    public Integer8BitElement(int byteAddress) {
        super(byteAddress, OpenemsType.INTEGER);
    }

    @Override
    public Integer getValue() {
        return ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).put(3, this.rawValue[0]).getInt(0);
    }

    @Override
    public void setValue(Integer value) {
        this.rawValue[0] = ByteBuffer.allocate(4).putInt(value).array()[0];
    }

    @Override
    public void setNextWriteValue(Optional<Integer> valueOpt) throws OpenemsException {

    }
}
