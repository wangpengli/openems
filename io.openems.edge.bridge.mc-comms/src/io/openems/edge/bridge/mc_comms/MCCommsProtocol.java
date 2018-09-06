package io.openems.edge.bridge.mc_comms;

import io.openems.edge.bridge.mc_comms.api.task.MCCommsTask;
import io.openems.edge.bridge.mc_comms.api.task.ReadMCCommsTask;
import io.openems.edge.bridge.mc_comms.api.task.WriteMCCommsTask;
import io.openems.edge.common.taskmanager.TaskManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class MCCommsProtocol {

    /**
     * TaskManager for ReadTasks
     */
    private final TaskManager<ReadMCCommsTask> readTaskManager = new TaskManager<>();

    /**
     * TaskManager for WriteTasks
     */
    private final TaskManager<WriteMCCommsTask> writeTaskManager = new TaskManager<>();


    private final Logger log = LoggerFactory.getLogger(MCCommsProtocol.class);

    private final int slaveAddress;

    public MCCommsProtocol(int slaveAddress, MCCommsTask... MCCommsTasks) {
        this.slaveAddress = slaveAddress;
        for (MCCommsTask mcCommsTask : MCCommsTasks) {
            mcCommsTask.setProtocol(this);
            addTask(mcCommsTask);
        }

    }

    public int getSlaveAddress() {
        return slaveAddress;
    }

    private synchronized void addTask(MCCommsTask MCCommsTask) {
        MCCommsTask.setDestinationAddress(this.slaveAddress);
        if (MCCommsTask instanceof WriteMCCommsTask) {
            this.writeTaskManager.addTask((WriteMCCommsTask) MCCommsTask);
        }
        if (MCCommsTask instanceof ReadMCCommsTask) {
            this.readTaskManager.addTask((ReadMCCommsTask) MCCommsTask);
        }

    }

    public List<ReadMCCommsTask> getNextReadTasks() {
        return null; //TODO
    }

    public List<WriteMCCommsTask> getNextWriteTasks() {
        return null; //TODO
    }
}
