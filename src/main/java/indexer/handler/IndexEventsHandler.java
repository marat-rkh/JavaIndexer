package indexer.handler;

import indexer.exceptions.NotHandledEventException;

import java.nio.file.Path;

/**
 * Created by mrx on 26.09.14.
 */
public interface IndexEventsHandler {
    public void onFilesAddedEvent(Path filePath) throws NotHandledEventException;
    public void onFilesRemovedEvent(Path filePath) throws NotHandledEventException;
    public void onFilesModifiedEvent(Path filePath) throws NotHandledEventException;
}
