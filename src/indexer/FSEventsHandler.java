package indexer;

import java.nio.file.Path;

/**
 * Created by mrx on 26.09.14.
 */
public interface FSEventsHandler {
    public void onCreateFileEvent(Path filePath);
    public void onRemoveFileEvent(Path filePath);
    public void onModifyFileEvent(Path filePath);
}
