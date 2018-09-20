package io.openems.edge.bridge.mc_comms.util;

import io.openems.edge.bridge.mc_comms.api.task.AbstractMCCommsTask;
import io.openems.edge.bridge.mc_comms.api.task.ReadMCCommsTask;
import io.openems.edge.bridge.mc_comms.api.task.WriteMCCommsTask;
import io.openems.edge.common.taskmanager.TaskManager;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class MCCommsProtocol {

    private final TaskManager<ReadMCCommsTask> readTaskManager = new TaskManager<>();
    private final TaskManager<WriteMCCommsTask> writeTaskManager = new TaskManager<>();
    private final AtomicReference<AbstractMCCommsComponent> parentComponentAtomicRef;

    public MCCommsProtocol(AtomicReference<AbstractMCCommsComponent> parentComponentAtomicRef, AbstractMCCommsTask... MCCommsTasks) {
        this.parentComponentAtomicRef = parentComponentAtomicRef;
        for (AbstractMCCommsTask task : MCCommsTasks) {
            task.setProtocol(this);
            addTask(task);
        }
    }

    public AtomicReference<AbstractMCCommsComponent> getParentComponentAtomicRef() {
        return parentComponentAtomicRef;
    }

    private synchronized void addTask(AbstractMCCommsTask MCCommsTask) {
        if (MCCommsTask instanceof WriteMCCommsTask) {
            this.writeTaskManager.addTask((WriteMCCommsTask) MCCommsTask);
        }
        if (MCCommsTask instanceof ReadMCCommsTask) {
            this.readTaskManager.addTask((ReadMCCommsTask) MCCommsTask);
        }

    }


    public List<ReadMCCommsTask> getNextReadTasks() {
        return this.readTaskManager.getNextReadTasks();
    }

    public List<WriteMCCommsTask> getNextWriteTasks() {
        return this.writeTaskManager.getNextReadTasks();
    }
}
