package io.openems.edge.bridge.mc_comms.api.task;

import io.openems.edge.bridge.mc_comms.MCCommsBridge;
import io.openems.edge.bridge.mc_comms.api.element.MCCommsElement;
import io.openems.edge.bridge.mc_comms.util.MCCommsException;
import io.openems.edge.bridge.mc_comms.util.MCCommsTXPacket;
import io.openems.edge.common.taskmanager.ManagedTask;
import io.openems.edge.common.taskmanager.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

public class WriteMCCommsTask extends AbstractMCCommsTask implements MCCommsTask, ManagedTask {
    private final boolean ackBeforeWrite;
    private boolean success;
    private final Logger logger = LoggerFactory.getLogger(WriteMCCommsTask.class);

    WriteMCCommsTask(int command, Priority priority, boolean ackBeforeWrite, MCCommsElement<?>... elements) {
        super(command, priority, elements);
        this.ackBeforeWrite = ackBeforeWrite;
    }

    @Override
    public void executeQuery(MCCommsBridge bridge) throws MCCommsException {
        //sends connection request before writing if ackBeforeWrite == true
        if (ackBeforeWrite) {
            this.sendCommandPacket(bridge, 0, reply -> {
                if (reply.getCommand() != 1) {
                    logger.debug("[MCCOMMS] Could not establish MCComms connection (reply command code: " + reply.getCommand() + ")");
                }
            });
        }
        MCCommsTXPacket packet = this.createPacket(bridge);
        this.sender.writePacket(packet);
    }

    private synchronized void setSuccess(boolean success) {
        this.success = success;
    }

    private MCCommsTXPacket createPacket(MCCommsBridge bridge) {
        ByteBuffer packetBuffer = ByteBuffer.allocate(15);
        for (MCCommsElement<?> element : this.elements) {
            packetBuffer.put(element._getRawValue().array(), element.getByteAddress(), element.getNumBytes());
        }
        return new MCCommsTXPacket(this.command, bridge.getMasterAddress(), this.getProtocol().getSlaveAddress(), packetBuffer.array());
    }
}
