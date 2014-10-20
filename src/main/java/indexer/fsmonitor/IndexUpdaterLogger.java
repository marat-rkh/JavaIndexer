package indexer.fsmonitor;

import indexer.exceptions.NotHandledEventException;
import indexer.handler.IndexEventsHandler;
import indexer.utils.Logger;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Base class for services that can handle fs events, affect FileIndex accordingly using
 * IndexEventsHandler and log these events
 *
 * @see indexer.fsmonitor.RootMonitor
 * @see indexer.fsmonitor.DirContentMonitor
 */
public abstract class IndexUpdaterLogger {
    protected final IndexEventsHandler indexEventsHandler;
    protected final Logger logger;

    protected IndexUpdaterLogger(IndexEventsHandler indexEventsHandler, Logger logger) {
        this.indexEventsHandler = indexEventsHandler;
        this.logger = logger;
    }

    protected void handleCreateEvent(Path path) throws NotHandledEventException {
        traceIfPossible("FS event 'create': " + path.toString());
        indexEventsHandler.onFilesAddedEvent(path);
    }
    protected void handleDeleteEvent(Path path) throws NotHandledEventException {
        traceIfPossible("FS event 'delete': " + path.toString());
        indexEventsHandler.onFilesRemovedEvent(path);
    }
    protected void handleModifyEvent(Path path) throws NotHandledEventException {
        traceIfPossible("FS event 'modify': " + path.toString());
        indexEventsHandler.onFilesModifiedEvent(path);
    }

    protected void traceIfPossible(String msg) {
        if(logger != null) {
            try {
                logger.log(msg);
            } catch (IOException e) {
                // suppressed
            }
        }
    }
}
