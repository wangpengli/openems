package io.openems.edge.bridge.mc_comms.util;

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


    public MCCommsProtocol(MCCommsTask... MCCommsTasks) {
        for (MCCommsTask mcCommsTask : MCCommsTasks) {
            mcCommsTask.setProtocol(this);
            addTask(mcCommsTask);
        }

    }

    private synchronized void addTask(MCCommsTask MCCommsTask) {
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
