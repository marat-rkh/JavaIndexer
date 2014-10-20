package indexer.fsmonitor;

import indexer.exceptions.NotHandledEventException;

import java.io.IOException;
import java.nio.file.Path;

/**
 * FSMonitorLifecycleHandler implementation for handling RootMonitor
 * shut down event. On this events happened {@link FSMonitorsManager#removeMonitor(java.nio.file.Path)}
 * method is called to remove stopped RootMonitor
 *
 * @see indexer.fsmonitor.RootMonitor
 */
public class RootMonitorHandler implements FSMonitorLifecycleHandler {
    private final FSMonitorsManager monitorsManager;
    private boolean monitorIsDown = false;

    public RootMonitorHandler(FSMonitorsManager monitorsManager) {
        this.monitorsManager = monitorsManager;
    }

    @Override
    public void onMonitorRestart(Path monitorsDirectory) throws NotHandledEventException {}

    @Override
    public void onMonitorDown(Path monitorsDirectory) throws NotHandledEventException {
        try {
            monitorsManager.removeMonitor(monitorsDirectory);
            monitorIsDown = true;
        } catch (IOException e) {
            throw new NotHandledEventException("Root monitor stop event not handled due to IO errors");
        }
    }

    @Override
    public boolean isMonitorDown() { return monitorIsDown; }
}
