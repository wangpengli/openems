package io.openems.edge.bridge.mc_comms.api.element;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.mc_comms.api.task.AbstractMCCommsTask;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public abstract class MCCommsElement<T> {

    protected int byteAddress;
    protected int numBytes;
    protected AbstractMCCommsTask mcCommsTask;
    protected OpenemsType type;
    protected byte[] rawValue;
    private final List<Consumer<T>> onUpdateCallbacks = new CopyOnWriteArrayList<>();

    public MCCommsElement(int byteAddress, int numBytes, OpenemsType type) {
        this.byteAddress = byteAddress;
        this.numBytes = numBytes;
        this.type = type;
    }

    /**
     * The onUpdateCallback is called on reception of a new value.
     *
     * Be aware, that this is the original, untouched value.
     * ChannelToElementConverters are not applied here!
     */
    public MCCommsElement<T> onUpdateCallback(Consumer<T> onUpdateCallback) {
        this.onUpdateCallbacks.add(onUpdateCallback);
        return this;
    }

    public ByteBuffer _getRawValue() {
        return ByteBuffer.wrap(rawValue.clone());
    }

    public abstract T getValue();

    public abstract void _setRawValue(ByteBuffer buffer);

    public abstract void setValue(T value);

    public int getByteAddress() {
        return byteAddress;
    }

    public int getNumBytes() {
        return numBytes;
    }

    public AbstractMCCommsTask getMcCommsTask() {
        return mcCommsTask;
    }

    public void setMcCommsTask(AbstractMCCommsTask mcCommsTask) {
        this.mcCommsTask = mcCommsTask;
    }

    public abstract OpenemsType getType();

    public abstract void _setNextWriteValue(Optional<T> valueOpt) throws OpenemsException;

}
