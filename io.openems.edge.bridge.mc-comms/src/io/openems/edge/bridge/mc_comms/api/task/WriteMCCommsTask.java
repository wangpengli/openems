package io.openems.edge.bridge.mc_comms.api.task;

import io.openems.edge.bridge.mc_comms.MCCommsBridge;
import io.openems.edge.bridge.mc_comms.api.element.MCCommsElement;
import io.openems.edge.bridge.mc_comms.util.MCCommsException;
import io.openems.edge.bridge.mc_comms.util.MCCommsPacket;
import io.openems.edge.common.taskmanager.ManagedTask;
import io.openems.edge.common.taskmanager.Priority;

import java.nio.ByteBuffer;

public class WriteMCCommsTask extends AbstractMCCommsTask implements ManagedTask {
    private final boolean ackBeforeWrite;

    WriteMCCommsTask(MCCommsBridge bridge, int slaveAddress, int command, Priority priority, boolean ackBeforeWrite, MCCommsElement<?>... elements) {
        super(bridge, slaveAddress, command, priority, elements);
        this.ackBeforeWrite = ackBeforeWrite;
    }

    @Override
    public void executeQuery() throws MCCommsException {
        //sends connection request before writing if ackBeforeWrite == true
        if (ackBeforeWrite) {
            bridge.IOPacketBuffer.TXPacketQueue.add(new MCCommsPacket(0, this.bridge.getMasterAddress(), this.slaveAddress));
            MCCommsPacket replyPacket = transferQueue.poll();
            if (replyPacket == null || replyPacket.getCommand() != 1) {
                throw new MCCommsException("[MCCOMMS] Could not establish MCComms connection (reply command code: " + (replyPacket != null ? replyPacket.getCommand() : "null") + ")");
            }
            ByteBuffer packetBuffer = ByteBuffer.allocate(15);
            for (MCCommsElement<?> element : this.elements) {
                packetBuffer.put(element._getRawValue().array(), element.getByteAddress(), element.getNumBytes());
            }
            bridge.IOPacketBuffer.TXPacketQueue.add(new MCCommsPacket(command, bridge.getMasterAddress(), slaveAddress, packetBuffer.array()));

        }
    }
}
