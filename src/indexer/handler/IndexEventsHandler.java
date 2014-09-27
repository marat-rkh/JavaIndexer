package indexer.handler;

import java.nio.file.Path;

/**
 * Created by mrx on 26.09.14.
 */
public interface IndexEventsHandler {
    public void onFilesAddedEvent(Path filePath);
    public void onFilesRemovedEvent(Path filePath);
    public void onFilesModifiedEvent(Path filePath);
}
