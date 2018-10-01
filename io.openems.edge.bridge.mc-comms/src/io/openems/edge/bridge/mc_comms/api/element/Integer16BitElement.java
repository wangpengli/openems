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
        ByteBuffer returnBuffer = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN);
        returnBuffer.position(3);
        returnBuffer.put(this.rawValue);
        return returnBuffer.getInt();
    }

    @Override
    public void setByteBuffer(ByteBuffer buffer) {
        this.rawValue = buffer.array();
    }

    @Override
    public void setValue(Integer value) {
        byte[] copyVal = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(value).array();
        this.rawValue[0] = copyVal[2];
        this.rawValue[1] = copyVal[3];
    }

    @Override
    public void setNextWriteValue(Optional<Integer> valueOpt) throws OpenemsException {

    }
}
