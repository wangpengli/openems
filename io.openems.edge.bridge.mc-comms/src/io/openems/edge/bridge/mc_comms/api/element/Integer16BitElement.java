package io.openems.edge.bridge.mc_comms.api.element;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Optional;

public class Integer16BitElement extends MCCommsElement<Integer> {
    public Integer16BitElement(int byteAddress) {
        super(byteAddress, 2, OpenemsType.INTEGER);
    }

    @Override
    public Integer getValue() {
        return ByteBuffer.wrap(this.rawValue).order(ByteOrder.BIG_ENDIAN).getInt();
    }

    @Override
    public void setByteBuffer(ByteBuffer buffer) {
        this.rawValue = buffer.array();
    }

    @Override
    public void setValue(Integer value) {
        this.rawValue = ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN).putInt(value).array();
    }

    @Override
    public void setNextWriteValue(Optional<Integer> valueOpt) throws OpenemsException {

    }
}
