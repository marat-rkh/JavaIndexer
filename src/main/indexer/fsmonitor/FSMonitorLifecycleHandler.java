package indexer.fsmonitor;

import indexer.exceptions.NotHandledEventException;

import java.nio.file.Path;

/**
 * Created by mrx on 29.09.14.
 */
public interface FSMonitorLifecycleHandler {
    public void onMonitorRestart(Path monitorsDirectory) throws NotHandledEventException;
    public void onMonitorDown(Path monitorsDirectory) throws NotHandledEventException;
}
