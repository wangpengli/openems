package io.openems.edge.bridge.mc_comms;

import com.google.common.collect.Multimap;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.mc_comms.api.task.ReadTask;
import io.openems.edge.bridge.mc_comms.api.task.WriteTask;
import io.openems.edge.common.worker.AbstractCycleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


public class MCCommsWorker extends AbstractCycleWorker {

    private final BridgeMCComms bridge;
    private final Logger logger = LoggerFactory.getLogger(MCCommsWorker.class);
    private final Multimap<String, MCCommsProtocol> protocols;

    MCCommsWorker(BridgeMCComms bridge) {
        this.bridge = bridge;
        this.protocols = bridge.getProtocols();
    }

    @Override
    public void activate(String name) {
        super.activate(name);
    }

    @Override
    public void deactivate() {
        super.deactivate();
    }

    @Override
    protected void forever() {
        // get the read tasks for this run
        List<ReadTask> nextReadTasks = this.getNextReadTasks();

        /*
         * execute next read tasks
         */
        nextReadTasks.forEach(readTask -> {
            /*
             * Execute next read abstractTask
             */
            try {
                readTask.executeQuery(bridge);
            } catch (OpenemsException e) {
                // TODO remember defective unitid
                logError(log, readTask.toString() + " read failed: " + e.getMessage());
            }
        });
    }

    /**
     * Returns the 'nextReadTasks' list.
     *
     * This checks if a device is listed as defective and - if it is - adds only one
     * abstractTask with this unitId to the queue
     */
    private List<ReadTask> getNextReadTasks() {
        List<ReadTask> result = new ArrayList<>();
        protocols.values().forEach(protocol -> {
            // get the next read tasks from the protocol
            List<ReadTask> nextReadTasks = protocol.getNextReadTasks();
            // check if the unitId is defective
            // int unitId = protocol.getUnitId();
            // FIXME: if we do the following in here, we will eventually miss the
            // ONCE-priority tasks
            // if (nextReadTasks.size() > 0 && defectiveUnitIds.contains(unitId)) {
            // // it is defective. Add only one read abstractTask.
            // // This avoids filling the queue with requests that cannot be fulfilled
            // anyway
            // // because the unitId is not reachable
            // result.add(nextReadTasks.get(0));
            // } else {
            // add all tasks to the next tasks
            result.addAll(nextReadTasks);
            // }
        });
        return result;
    }

    private List<WriteTask> getNextWriteTasks() {
        List<WriteTask> result = new ArrayList<>();
        protocols.values().forEach(protocol -> {
            result.addAll(protocol.getNextWriteTasks());
        });
        return result;
    }
}
