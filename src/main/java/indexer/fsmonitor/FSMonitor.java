package indexer.fsmonitor;

import indexer.exceptions.NotHandledEventException;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Represents monitor that listens some directory for files adding, removing and modification events
 * or file for removing or modification events.
 */
public interface FSMonitor {
    public Path getTarget();
    public void startMonitoring() throws NotHandledEventException;
    public void stopMonitoring() throws IOException;
}