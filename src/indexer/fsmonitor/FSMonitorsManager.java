package indexer.fsmonitor;

import indexer.exceptions.NotHandledEventException;
import indexer.handler.IndexEventsHandler;
import indexer.utils.PathUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by mrx on 27.09.14.
 */
public class FSMonitorsManager {
    private final Map<Path, FSMonitor> monitors = new HashMap<Path, FSMonitor>();
    private final IndexEventsHandler indexEventsHandler;
    private final FSMonitorLifecycleHandler monitorLifecycleHandler;
    private final OutputStream traceStream;

    private boolean errorOccurred = false;

    public FSMonitorsManager(IndexEventsHandler indexEventsHandler, FSMonitorLifecycleHandler monitorHandler,
                             OutputStream traceStream) {
        this.indexEventsHandler = indexEventsHandler;
        this.monitorLifecycleHandler = monitorHandler;
        this.traceStream = traceStream;
    }

    public synchronized boolean addMonitor(Path directory, int restartsCounter) throws IOException {
        if(addingIsNeeded(directory)) {
            try {
                FSMonitor newMonitor = new SingleDirMonitor(directory, indexEventsHandler, traceStream);
                monitors.put(directory, newMonitor);
                Thread monitorThread = new Thread(new MonitorRunner(newMonitor, restartsCounter));
                monitorThread.start();
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    public synchronized boolean removeMonitor(Path directory) throws IOException {
        FSMonitor monitor = monitors.get(directory);
        if(monitor != null) {
            monitor.stopMonitoring();
            monitors.remove(directory);
            return true;
        }
        return false;
    }

    public synchronized void stopAllMonitors() throws IOException {
        for(FSMonitor fsMonitor : monitors.values()) {
            fsMonitor.stopMonitoring();
        }
    }

    public boolean isErrorOccurred() {
        return errorOccurred;
    }

    private boolean addingIsNeeded(Path dirToAdd) throws IOException {
        Iterator<Map.Entry<Path, FSMonitor>> it = monitors.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry<Path, FSMonitor> entry = it.next();
            Path currentPath = entry.getKey();
            if(PathUtils.pathsAreEqual(currentPath, dirToAdd) || PathUtils.firstPathIsParent(currentPath, dirToAdd)) {
                return false;
            } else if(PathUtils.firstPathIsParent(dirToAdd, currentPath)) {
                entry.getValue().stopMonitoring();
                it.remove();
            }
        }
        return true;
    }

    private class MonitorRunner implements Runnable {
        private final FSMonitor monitor;
        private int restartsCounter;

        private MonitorRunner(FSMonitor monitor, int restartsCounter) {
            this.monitor = monitor;
            this.restartsCounter = restartsCounter;
        }

        @Override
        public void run() {
            try {
                monitor.startMonitoring();
            } catch (NotHandledEventException e) {
                restartsCounter -= 1;
                try {
                    if (restartsCounter >= 0) {
                        monitorLifecycleHandler.onMonitorRestart(monitor.getDirectory());
                        run();
                    } else {
                        monitorLifecycleHandler.onMonitorDown(monitor.getDirectory());
                    }
                } catch (NotHandledEventException ex) {
                    errorOccurred = true;
                }
            }
        }
    }
}
