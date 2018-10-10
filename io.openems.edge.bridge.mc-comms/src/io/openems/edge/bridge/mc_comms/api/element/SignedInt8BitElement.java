package io.openems.edge.bridge.mc_comms.api.element;

import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.mc_comms.util.MCCommsException;

import java.nio.ByteBuffer;

public class SignedInt8BitElement extends Abstract8BitElement<Integer> {

    /**
     * Holds a signed integer value between +128 and -127
     * @param byteAddress
     */
    public SignedInt8BitElement(int byteAddress) {
        super(byteAddress, OpenemsType.INTEGER);
    }

    @Override
    public Integer getValue() {
        return ((int) rawValue[0]);
    }

    @Override
    public void setValue(Integer value) throws MCCommsException {
        if (value < 129 && value > -128) {
            if (value < 0) {
                rawValue[0] = ByteBuffer.allocate(4).putInt(256 - ByteBuffer.allocate(4).putInt(value).get(3)).get(3);
            } else {
                rawValue[0] = ByteBuffer.allocate(4).putInt(value).get(3);
            }
        } else {
            throw new MCCommsException("value must not exceed +128 or -127 for signed 8 bit values");
        }
    }
}
