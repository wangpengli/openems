package io.openems.edge.bridge.mc_comms.api.task;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.mc_comms.MCCommsBridge;
import io.openems.edge.bridge.mc_comms.api.element.MCCommsElement;
import io.openems.edge.bridge.mc_comms.util.*;
import io.openems.edge.common.taskmanager.ManagedTask;
import io.openems.edge.common.taskmanager.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ReadMCCommsTask extends AbstractMCCommsTask implements MCCommsTask, ManagedTask {

    private final int expectedReplyCommand;
    private final Logger logger = LoggerFactory.getLogger(MCCommsTask.class);

    public ReadMCCommsTask(int readCommand, int expectedReplyCommand, Priority priority, MCCommsElement<?>... elements) {
        super(readCommand, priority);
        this.expectedReplyCommand = expectedReplyCommand;
    }

    /**
     * Sends a query for this AbstractMCCommsTask to the MCComms device
     *
     * @param bridge
     * @throws OpenemsException
     */
    @Override
    public void executeQuery(MCCommsBridge bridge) throws MCCommsException
    {
        sendCommandPacket(bridge, this.command, packet -> {
            try {
                handlePacket(packet);
            } catch (MCCommsException e) {
                logger.debug(e.getMessage());
            }
        });
    }

    private void handlePacket(MCCommsRXPacket packet) throws MCCommsException {
        //check command response
        int receivedCommand = packet.getCommand();
        if (packet.getCommand() != this.expectedReplyCommand) {
            throw new MCCommsException("[MCCOMMS] Unexpected command! Expecting command code [" + this.expectedReplyCommand + "] but got command [" + receivedCommand);
        }
        //retrieve payload
        byte[] payload = packet.getPayload();
        //assign payload bytes to elements
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
    }
}
