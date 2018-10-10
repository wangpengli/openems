package io.openems.edge.bridge.mc_comms.api.element;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.mc_comms.api.task.AbstractMCCommsTask;
import io.openems.edge.bridge.mc_comms.util.MCCommsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Abstract element for MC Comms components
 * @param <T> the element type - generally integer or boolean
 */
public abstract class MCCommsElement<T> {

    protected int byteAddress;
    protected int numBytes;
    protected AbstractMCCommsTask mcCommsTask;
    protected OpenemsType type;
    protected byte[] rawValue;
    private final List<Consumer<T>> onUpdateCallbacks = new CopyOnWriteArrayList<>();
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Constructor
     * @param byteAddress the offset for the first byte of the element value from the read packet
     * @param numBytes the number of bytes containing the value
     * @param type OpenEMS type of the value
     */
    public MCCommsElement(int byteAddress, int numBytes, OpenemsType type) {
        this.rawValue = new byte[numBytes];
        this.byteAddress = byteAddress;
        this.numBytes = numBytes;
        this.type = type;
    }

    /**
     * Constructor with size specification for internal buffer
     * @param byteAddress the offset for the first byte of the element value from the read packet
     * @param numBytes the number of bytes containing the value
     * @param numRawBytes size of internal buffer
     * @param type OpenEMS type of the value
     */
    public MCCommsElement(int byteAddress, int numBytes, int numRawBytes, OpenemsType type) {
        this.rawValue = new byte[numRawBytes];
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

    /**
     * @return the raw byte values as a ByteBuffer
     */
    public ByteBuffer getByteBuffer() {
        return ByteBuffer.wrap(rawValue.clone());
    }

    /**
     * @return the value of the element as the element type
     */
    public abstract T getValue();

    /**
     * @param buffer the raw byte values to be set, as a bytebuffer
     */
    public abstract void setByteBuffer(ByteBuffer buffer);

    /**
     * Sets the raw value of the element from a value that has the same type as the element
     * @param value the value to be set
     * @throws MCCommsException generally if a value is out of bounds
     */
    public abstract void setValue(T value) throws MCCommsException;

    /**
     * return the byte offset of this element
     * @return byteAddress
     */
    public int getByteAddress() {
        return byteAddress;
    }

    /**
     * @return number of bytes this element maps to
     */
    public int getNumBytes() {
        return numBytes;
    }

    /**
     * @return the MCCommsTask this element belongs to
     */
    public AbstractMCCommsTask getMcCommsTask() {
        return mcCommsTask;
    }

    /**
     * @param mcCommsTask the owning MCCommsTask for this element
     */
    public void setMcCommsTask(AbstractMCCommsTask mcCommsTask) {
        this.mcCommsTask = mcCommsTask;
    }

    /**
     * @return the Type of this element
     */
    public OpenemsType getType() {
        return this.type;
    }

    /**
     * sets the value to be written to this element, if one is present
     * @param valueOpt optional value
     * @throws OpenemsException idk lol
     */
    public void setNextWriteValue(Optional<T> valueOpt) throws OpenemsException {
        valueOpt.ifPresent(value -> {
            try {
                setValue(value);
            } catch (MCCommsException e) {
                logger.error(e.getMessage());
            }
        });
    }

}
