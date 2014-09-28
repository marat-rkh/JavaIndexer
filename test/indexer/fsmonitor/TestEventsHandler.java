package indexer.fsmonitor;

import indexer.handler.IndexEventsHandler;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mrx on 28.09.14.
 */
public class TestEventsHandler implements IndexEventsHandler {
    public final List<File> addedPaths = new ArrayList<File>();
    public final List<File> removedPaths = new ArrayList<File>();
    public final List<File> modifiedPaths = new ArrayList<File>();

    @Override
    public void onFilesAddedEvent(Path filePath) {
        addedPaths.add(filePath.toFile());
    }

    @Override
    public void onFilesRemovedEvent(Path filePath) {
        removedPaths.add(filePath.toFile());
    }

    @Override
    public void onFilesModifiedEvent(Path filePath) {
        modifiedPaths.add(filePath.toFile());
    }
}
