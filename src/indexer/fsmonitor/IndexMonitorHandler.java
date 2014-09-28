package indexer.fsmonitor;

import indexer.exceptions.NotHandledEventException;
import indexer.handler.IndexEventsHandler;

import java.nio.file.Path;

/**
 * Created by mrx on 29.09.14.
 */
public class IndexMonitorHandler implements FSMonitorLifecycleHandler {
    private IndexEventsHandler indexEventsHandler;
    private boolean monitorIsDown = false;

    public IndexMonitorHandler(IndexEventsHandler indexEventsHandler) {
        this.indexEventsHandler = indexEventsHandler;
    }

    @Override
    public void onMonitorRestart(Path monitorsDirectory) throws NotHandledEventException {
        indexEventsHandler.onFilesRemovedEvent(monitorsDirectory);
        indexEventsHandler.onFilesAddedEvent(monitorsDirectory);
    }

    @Override
    public void onMonitorDown(Path monitorsDirectory) throws NotHandledEventException {
        indexEventsHandler.onFilesRemovedEvent(monitorsDirectory);
        monitorIsDown = true;
    }

    public boolean isSomeMonitorDown() {
        return monitorIsDown;
    }
}
