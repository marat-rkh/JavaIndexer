package indexer.fsmonitor;

import indexer.exceptions.NotHandledEventException;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by mrx on 26.09.14.
 */
public interface FSMonitor {
    public Path getDirectory();
    public void startMonitoring() throws NotHandledEventException;
    public void stopMonitoring() throws IOException;
}