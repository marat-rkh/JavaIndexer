package indexer.fsmonitor;

import indexer.exceptions.NotHandledEventException;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Represents monitor that listens some directory for files adding, removing and modification events.
 * For listening WatchService class is used.
 *
 * @see java.nio.file.WatchService
 */
public interface FSMonitor {
    public Path getDirectory();
    public void startMonitoring() throws NotHandledEventException;
    public void stopMonitoring() throws IOException;
}