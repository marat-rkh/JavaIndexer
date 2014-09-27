package indexer.fsmonitor;

import indexer.FSEventsHandler;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by mrx on 27.09.14.
 */
public class FSMonitoringManager {
    private final List<FSMonitor> monitors = new LinkedList<FSMonitor>();
    private final FSEventsHandler fsEventsHandler;

    public FSMonitoringManager(FSEventsHandler fsEventsHandler) {
        this.fsEventsHandler = fsEventsHandler;
    }

    public boolean addMonitor(Path directory) {
        try {
            final FSMonitor newMonitor = new SingleDirMonitor(fsEventsHandler, directory);
            monitors.add(newMonitor);
            Thread monitorThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    newMonitor.startMonitoring();
                }
            });
            monitorThread.start();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public void stopAllMonitors() {
        for(FSMonitor fsMonitor : monitors) {
            fsMonitor.stopMonitoring();
        }
    }
}
