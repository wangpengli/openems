package io.openems.edge.bridge.mc_comms.api.task;

import io.openems.edge.bridge.mc_comms.MCCommsBridge;
import io.openems.edge.bridge.mc_comms.api.element.MCCommsElement;
import io.openems.edge.bridge.mc_comms.util.AbstractMCCommsComponent;
import io.openems.edge.bridge.mc_comms.util.MCCommsException;
import io.openems.edge.bridge.mc_comms.util.MCCommsPacket;
import io.openems.edge.bridge.mc_comms.util.MCCommsProtocol;
import io.openems.edge.common.taskmanager.ManagedTask;
import io.openems.edge.common.taskmanager.Priority;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Task for reading values from and MC Comms slave
 */
public class ReadMCCommsTask extends AbstractMCCommsTask implements ManagedTask {

    private final int expectedReplyCommand;
    private final int readReplyTimeoutMs;

    /**
     * Constructor
     * @param readCommand Command integer to be sent to the slave device
     * @param expectedReplyCommand Command integer expected in reply packet
     * @param priority priority level of this task
     * @param elements MCCommsElements to be mapped, usually in the form of a ChannelMapper
     */
    public ReadMCCommsTask(int readCommand, int expectedReplyCommand, Priority priority, MCCommsElement<?>... elements) {
        super(readCommand, priority, elements);
        this.expectedReplyCommand = expectedReplyCommand;
        readReplyTimeoutMs = 100;
    }

    /**
     * Constructor with timeout
     * @param readCommand Command integer to be sent to the slave device
     * @param expectedReplyCommand Command integer expected in reply packet
     * @param priority priority level of this task
     * @param readReplyTimeoutMs number of milliseconds to wait until no reply packet is to be expected
     * @param elements MCCommsElements to be mapped, usually in the form of a ChannelMapper
     */
    public ReadMCCommsTask(int readCommand, int expectedReplyCommand, Priority priority, int readReplyTimeoutMs, MCCommsElement<?>... elements) {
        super(readCommand, priority, elements);
        this.expectedReplyCommand = expectedReplyCommand;
        this.readReplyTimeoutMs = readReplyTimeoutMs;
    }



    /**
     * Sends a query for this ReadTask to the MCComms device
     *
     * @throws MCCommsException command mismatch
     */
    @Override
    public void executeQuery() throws MCCommsException {
        MCCommsProtocol protocol = this.getProtocol();
        AbstractMCCommsComponent parentComponent = protocol.getParentComponentAtomicRef().get();
        MCCommsBridge bridge = parentComponent.getMCCommsBridgeAtomicRef().get();
        int slaveAddress = parentComponent.getSlaveAddress();
        bridge.getTXPacketQueue().add(new MCCommsPacket(this.command, bridge.getMasterAddress(), slaveAddress));
        MCCommsPacket commandReplyPacket = parentComponent.getPacket(expectedReplyCommand, readReplyTimeoutMs);
        if (commandReplyPacket != null) {
            //retrieve payload
            int[] payload = commandReplyPacket.getPayload();
            //assign payload bytes to elements
            for (MCCommsElement<?> element : this.elements) {
                int byteAddress, numBytes;
                byteAddress = element.getByteAddress();
                numBytes = element.getNumBytes();
                ByteBuffer elementBuffer = ByteBuffer.allocate(numBytes);
                for (int i = byteAddress; i <= (byteAddress + numBytes); i++) {
                    elementBuffer.order(ByteOrder.BIG_ENDIAN).put((byte) payload[i]);
                }
                element.setByteBuffer(elementBuffer);
            }

        } else {
            throw new MCCommsException("[MCCOMMS] Unexpected command! Expecting command code [" + this.expectedReplyCommand + "] but got command [" + commandReplyPacket.getCommand());
        }
    }
}
