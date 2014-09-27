package indexer.fsmonitor;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by mrx on 26.09.14.
 */
public interface FSMonitor {
    public void startMonitoring();
    public void stopMonitoring();
}