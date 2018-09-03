package io.openems.edge.bridge.mc_comms;

import io.openems.edge.bridge.mc_comms.api.task.*;
import io.openems.edge.common.taskmanager.TaskManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MCCommsProtocol {

    /**
     * TaskManager for ReadTasks
     */
    private final TaskManager<ReadTask> readTaskManager = new TaskManager<>();

    /**
     * TaskManager for WriteTasks
     */
    private final TaskManager<WriteTask> writeTaskManager = new TaskManager<>();


    private final Logger log = LoggerFactory.getLogger(MCCommsProtocol.class);

    private final int slaveAddress;

    public MCCommsProtocol(int slaveAddress, Task... tasks) {
        this.slaveAddress = slaveAddress;
        for (Task task : tasks) {
            addTask(task);
        }

    }

    private synchronized void addTask(Task task) {
        task.setSlaveAddress(this.slaveAddress);
        if (task instanceof WriteTask) {
            this.writeTaskManager.addTask((WriteTask) task);
        }
        if (task instanceof ReadTask) {
            this.readTaskManager.addTask((ReadTask) task);
        }

    }

}
