package indexer.handler;

import indexer.exceptions.NotHandledEventException;

import java.nio.file.Path;

/**
 * Interface representing index modification events handler. If adding, removing or
 * modifying files or folders events happen somewhere (for example, in filesystem),
 * appropriate methods of this handler can be called to affect index
 *
 * @see indexer.index.FileIndex
 */
public interface IndexEventsHandler {
    public void onFilesAddedEvent(Path filePath) throws NotHandledEventException;
    public void onFilesRemovedEvent(Path filePath) throws NotHandledEventException;
    public void onFilesModifiedEvent(Path filePath) throws NotHandledEventException;
}
