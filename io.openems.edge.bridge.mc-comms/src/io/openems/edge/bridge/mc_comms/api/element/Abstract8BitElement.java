package io.openems.edge.bridge.mc_comms.api.element;

import io.openems.common.types.OpenemsType;

import java.nio.ByteBuffer;

public abstract class Abstract8BitElement<T> extends MCCommsElement<T>{

    Abstract8BitElement(int byteAddress, OpenemsType type) {
        super(byteAddress, 1, type);
    }

    @Override
    public void setByteBuffer(ByteBuffer buffer) {
        this.rawValue[0] = buffer.get(buffer.capacity() - 1 );
    }
}
