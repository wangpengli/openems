package io.openems.edge.bridge.mc_comms.api.task;

import io.openems.edge.bridge.mc_comms.MCCommsBridge;
import io.openems.edge.bridge.mc_comms.api.element.MCCommsElement;
import io.openems.edge.bridge.mc_comms.util.MCCommsException;
import io.openems.edge.bridge.mc_comms.util.MCCommsTXPacket;
import io.openems.edge.common.taskmanager.ManagedTask;
import io.openems.edge.common.taskmanager.Priority;

import java.nio.ByteBuffer;

public class WriteMCCommsTask extends AbstractMCCommsTask implements MCCommsTask, ManagedTask {
    private final boolean ackBeforeWrite;

    WriteMCCommsTask(MCCommsBridge bridge, int slaveAddress, int command, Priority priority, boolean ackBeforeWrite, MCCommsElement<?>... elements) {
        super(bridge, slaveAddress, command, priority, elements);
        this.ackBeforeWrite = ackBeforeWrite;
    }

    @Override
    public void executeQuery() throws MCCommsException {
        //sends connection request before writing if ackBeforeWrite == true
        int replyCommand = 1;
        if (ackBeforeWrite) {
            this.packetHandler.writePacket(this.bridge, new MCCommsTXPacket(0, this.bridge.getMasterAddress(), this.slaveAddress));
            replyCommand = this.packetHandler.readReply(this.slaveAddress, this.bridge).getCommand();
        }
        if (replyCommand != 1)
            throw new MCCommsException("[MCCOMMS] Could not establish MCComms connection (reply command code: " + replyCommand + ")");
        ByteBuffer packetBuffer = ByteBuffer.allocate(15);
        for (MCCommsElement<?> element : this.elements) {
            packetBuffer.put(element._getRawValue().array(), element.getByteAddress(), element.getNumBytes());
        }
        this.packetHandler.writePacket(bridge, new MCCommsTXPacket(this.command, bridge.getMasterAddress(), this.slaveAddress, packetBuffer.array()));
    }
}
