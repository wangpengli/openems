package io.openems.edge.bridge.mc_comms.api.element;

import io.openems.common.types.OpenemsType;

import java.nio.ByteBuffer;

/**
 * Element representing a boolean value. Any nonzero read value is treated as 'true'
 */
public class BooleanElement extends Abstract8BitElement<Boolean> {
    public BooleanElement(int byteAddress) {
        super(byteAddress, OpenemsType.BOOLEAN);
    }

    @Override
    public Boolean getValue() {
        return this.rawValue[0] != 0;
    }

    @Override
    public void setValue(Boolean value) {
        if (value) {
            ByteBuffer.wrap(this.rawValue).put((byte) 1);
        }
        ByteBuffer.wrap(this.rawValue).put((byte) 0);
    }
}
