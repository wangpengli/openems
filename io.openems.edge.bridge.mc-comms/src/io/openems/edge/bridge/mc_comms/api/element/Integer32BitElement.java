package io.openems.edge.bridge.mc_comms.api.element;

import io.openems.common.types.OpenemsType;

import java.nio.ByteBuffer;

public class Integer32BitElement extends MCCommsElement<Integer> {
    public Integer32BitElement(int byteAddress) {
        super(byteAddress, 4, OpenemsType.INTEGER);
    }

    @Override
    public Integer getValue() {
        return ByteBuffer.wrap(rawValue).getInt(0);
    }

    @Override
    public void setByteBuffer(ByteBuffer buffer) {
        buffer.get(rawValue);
    }

    @Override
    public void setValue(Integer value) {
        this.rawValue = ByteBuffer.allocate(4).putInt(value).array();
    }
}
