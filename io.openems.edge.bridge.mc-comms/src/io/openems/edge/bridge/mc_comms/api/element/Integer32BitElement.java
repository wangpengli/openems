package io.openems.edge.bridge.mc_comms.api.element;

import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.mc_comms.util.MCCommsException;

import java.nio.ByteBuffer;

/**
 * Represents and unsigned 32 bit integer value
 */
public class Integer32BitElement extends MCCommsElement<Long> {
    public Integer32BitElement(int byteAddress) {
        super(byteAddress, 4, OpenemsType.INTEGER);
    }

    @Override
    public Long getValue() {
        return ByteBuffer.wrap(rawValue).getLong(0);
    }

    @Override
    public void setByteBuffer(ByteBuffer buffer) {
        this.rawValue = buffer.get(rawValue).array();
    }

    @Override
    public void setValue(Long value) throws MCCommsException {
        if (value >= 0) {
            ByteBuffer.wrap(this.rawValue).putLong(value);
        } else {
            throw new MCCommsException("Value must not be negative");
        }
    }
}
