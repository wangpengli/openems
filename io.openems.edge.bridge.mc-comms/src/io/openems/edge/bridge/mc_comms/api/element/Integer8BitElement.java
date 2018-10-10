package io.openems.edge.bridge.mc_comms.api.element;

import io.openems.common.types.OpenemsType;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * represents an unsigned 8 bit integer value element, can hold 0-255
 */
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
}
