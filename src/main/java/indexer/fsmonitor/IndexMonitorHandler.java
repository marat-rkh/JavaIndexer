package indexer.fsmonitor;

import indexer.exceptions.NotHandledEventException;
import indexer.handler.IndexEventsHandler;

import java.nio.file.Path;

/**
 * FSMonitorLifecycleHandler interface implementation for affection FileIndex on monitor restart
 * or shut down events
 *
 * @see indexer.fsmonitor.FSMonitorLifecycleHandler
 * @see indexer.index.FileIndex
 * @see indexer.handler.IndexEventsHandler
 */
public class IndexMonitorHandler implements FSMonitorLifecycleHandler {
    private IndexEventsHandler indexEventsHandler;
    private boolean monitorIsDown = false;

    public IndexMonitorHandler(IndexEventsHandler indexEventsHandler) {
        this.indexEventsHandler = indexEventsHandler;
    }

    /**
     * Re-adds files of directory corresponding to restarted monitor in FSIndex using
     * IndexEventsHandler passed in constructor
     *
     * @param monitorsDirectory directory corresponding to restarted monitor
     * @throws NotHandledEventException if re-adding fails
     */
    @Override
    public void onMonitorRestart(Path monitorsDirectory) throws NotHandledEventException {
        indexEventsHandler.onFilesRemovedEvent(monitorsDirectory);
        indexEventsHandler.onFilesAddedEvent(monitorsDirectory);
    }

    /**
     * Removes files of directory corresponding to shut down monitor from FSIndex using
     * IndexEventsHandler passed in constructor
     *
     * @param monitorsDirectory directory corresponding to shut down monitor
     * @throws NotHandledEventException if removing fails
     */
    @Override
    public void onMonitorDown(Path monitorsDirectory) throws NotHandledEventException {
        indexEventsHandler.onFilesRemovedEvent(monitorsDirectory);
        monitorIsDown = true;
    }

    /**
     * Checks if onMonitorDown method has been called at least once
     *
     * @return {@code true} if onMonitorDown method has been called at least once
     *         {@code false} otherwise
     */
    public boolean isSomeMonitorDown() {
        return monitorIsDown;
    }
}
