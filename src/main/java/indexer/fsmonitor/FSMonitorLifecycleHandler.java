package indexer.fsmonitor;

import indexer.exceptions.NotHandledEventException;

import java.nio.file.Path;

/**
 * Represents FSMonitor lifecycle events handler. If monitor restart or monitor shoot down
 * events happen appropriate methods of this interface can be called to affect some objects
 * or just trace this events
 *
 * @see indexer.fsmonitor.FSMonitor
 */
public interface FSMonitorLifecycleHandler {
    public void onMonitorRestart(Path monitorsDirectory) throws NotHandledEventException;
    public void onMonitorDown(Path monitorsDirectory) throws NotHandledEventException;
    public boolean isMonitorDown();
}
