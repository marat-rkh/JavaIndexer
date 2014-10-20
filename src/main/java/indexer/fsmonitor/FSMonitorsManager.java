package indexer.fsmonitor;

import indexer.exceptions.NotHandledEventException;
import indexer.handler.IndexEventsHandler;
import indexer.utils.Logger;
import indexer.utils.PathUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Class represents FSMonitors manager. It can add and start monitors for different folders and files
 * in separate threads, stop and remove them and try to restart monitors if they are failed
 * using FSMonitorLifecycleHandler to make appropriate changes on this events.
 * All operations are thread safe, only one adding or removing can be performed at a time.
 *
 * @see indexer.fsmonitor.FSMonitor
 * @see indexer.fsmonitor.FSMonitorLifecycleHandler
 */
public class FSMonitorsManager {
    private final Map<Path, FSMonitor> monitors = new HashMap<Path, FSMonitor>();
    private final IndexEventsHandler indexEventsHandler;
    private final FSMonitorLifecycleHandler monitorLifecycleHandler;
    private final Logger logger;

    private boolean errorOccurred = false;

    public FSMonitorsManager(IndexEventsHandler indexEventsHandler, FSMonitorLifecycleHandler monitorHandler,
                             Logger logger) {
        this.indexEventsHandler = indexEventsHandler;
        this.monitorLifecycleHandler = monitorHandler;
        this.logger = logger;
    }

    /**
     * Synchronously adds and starts new monitor for specified folder or file with eventsHandler passed
     * in constructor. Monitor is restarted {@code restartsCounter} times on fail before being
     * completely stopped.
     *
     * @param target directory or file to listen with new monitor
     * @param restartsCounter number of times monitor is restarted on fail
     * @return                {@code true} if monitor has been added and started or if monitoring
     *                        is not needed because passed target is already watched by some monitor
     *                        {@code false} if some errors occurred on monitor creation
     * @throws IOException
     */
    public synchronized boolean addMonitor(Path target, int restartsCounter) throws IOException {
        if(addingIsNeeded(target)) {
            try {
                FSMonitor newMonitor = new DirMonitor(target, indexEventsHandler, logger,
                                                      new RootMonitorHandler(this));
                monitors.put(target, newMonitor);
                Thread monitorThread = new Thread(new MonitorRunner(newMonitor, restartsCounter));
                monitorThread.start();
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    /**
     * Synchronously stops and removes monitor for specified target
     *
     * @param target directory or file to be stopped listening
     * @return          {@code true} if target's monitor has been removed
     *                  {@code false} if no monitor exactly for specified target (but some enclosing
     *                  monitor can present)
     * @throws IOException if IO errors occurred while stopping monitor
     */
    public synchronized boolean removeMonitor(Path target) throws IOException {
        FSMonitor monitor = monitors.get(target);
        if(monitor != null) {
            monitor.stopMonitoring();
            monitors.remove(target);
            return true;
        }
        return false;
    }

    /**
     * Synchronously stops all the monitors
     *
     * @throws IOException if IO errors occurred while stopping monitor
     */
    public synchronized void stopAllMonitors() throws IOException {
        for(FSMonitor fsMonitor : monitors.values()) {
            fsMonitor.stopMonitoring();
        }
    }

    /**
     * Checks if some error in monitors work occurred.
     *
     * @return {@code true} if FSMonitorLifecycleHandler methods calls failed on
     *         monitor restarting or monitor shooting down, {@code false} otherwise
     */
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
                        monitorLifecycleHandler.onMonitorRestart(monitor.getTarget());
                        run();
                    } else {
                        monitorLifecycleHandler.onMonitorDown(monitor.getTarget());
                    }
                } catch (NotHandledEventException ex) {
                    errorOccurred = true;
                }
            }
        }
    }
}
