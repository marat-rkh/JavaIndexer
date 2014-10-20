package indexer.fsmonitor;

import indexer.exceptions.NotHandledEventException;
import indexer.handler.IndexEventsHandler;
import indexer.utils.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * FSMonitor interface implementation for listening single directory or single file for
 * remove events and affecting index. On remove event detected and handled this
 * listener becomes useless and finishes it's work calling
 * {@link FSMonitorLifecycleHandler#onMonitorDown(java.nio.file.Path)} to handle this
 * lifecycle event.
 * Class takes IndexEventsHandler as constructor argument and calls appropriate methods on
 * remove events. Moreover, some output stream can be passed to trace events.
 */
public class RootMonitor extends IndexUpdaterLogger implements FSMonitor {
    private final File target;
    private final int CHECK_PERIOD = 1000;
    private final FSMonitorLifecycleHandler monitorHandler;

    private boolean isStopped = false;

    public RootMonitor(Path targetPath, IndexEventsHandler indexEventsHandler, Logger logger,
                       FSMonitorLifecycleHandler monitorHandler) throws IOException {
        super(indexEventsHandler, logger);
        this.target = targetPath.toFile();
        this.monitorHandler = monitorHandler;
    }

    @Override
    public Path getTarget() {
        return Paths.get(target.getAbsolutePath());
    }

    @Override
    public void startMonitoring() throws NotHandledEventException {
        isStopped = false;
        startRootCheckLoop();
        monitorHandler.onMonitorDown(Paths.get(target.getAbsolutePath()));
    }

    @Override
    public void stopMonitoring() throws IOException {
        isStopped = true;
    }

    private void startRootCheckLoop() throws NotHandledEventException {
        while(!isStopped) {
            if(!target.exists()) {
                handleDeleteEvent(Paths.get(target.getAbsolutePath()));
                isStopped = true;
                return;
            }
            try {
                Thread.sleep(CHECK_PERIOD);
            } catch (InterruptedException e) {
                isStopped = true;
                return;
            }
        }
    }
}
