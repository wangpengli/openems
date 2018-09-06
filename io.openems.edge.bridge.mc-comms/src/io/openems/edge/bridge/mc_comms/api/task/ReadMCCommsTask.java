package io.openems.edge.bridge.mc_comms.api.task;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.mc_comms.MCCommsBridge;
import io.openems.edge.bridge.mc_comms.MCCommsPacketListener;
import io.openems.edge.bridge.mc_comms.api.element.MCCommsElement;
import io.openems.edge.common.taskmanager.ManagedTask;
import io.openems.edge.common.taskmanager.Priority;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ReadMCCommsTask extends AbstractMCCommsTask implements MCCommsTask, ManagedTask {

    private MCCommsPacketListener listener;
    private byte[] payload;

    public ReadMCCommsTask(Priority priority, MCCommsElement<?>... elements) {
        super(priority, elements);
    }

    /**
     * Sends a query for this AbstractMCCommsTask to the MCComms device
     *
     * @param bridge
     * @throws OpenemsException
     */
    public <T> void executeQuery(MCCommsBridge bridge) throws OpenemsException
    {
        this.listener = new MCCommsPacketListener(this.getProtocol().getSlaveAddress(), bridge);
        this.listener.addOnReadCallback(payload -> {
            this.payload = payload;
            for (MCCommsElement<?> element : this.elements) {
                int byteAddress, numBytes;
                byteAddress = element.getByteAddress();
                numBytes = element.getNumBytes();
                ByteBuffer buffer = ByteBuffer.allocate(numBytes);
                for (int i = byteAddress; i < (byteAddress + numBytes); i++) {
                    buffer.order(ByteOrder.BIG_ENDIAN).put(payload[i]);
                }
                element._setRawValue(buffer);
            }
        });
    }
}
