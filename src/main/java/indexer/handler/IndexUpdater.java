package indexer.handler;

import indexer.exceptions.InconsistentIndexException;
import indexer.exceptions.NotHandledEventException;
import indexer.index.FileIndex;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * IndexEventsHandler interface implementation
 *
 * @see indexer.handler.IndexEventsHandler
 */
public class IndexUpdater implements IndexEventsHandler {
    private final FileIndex fileIndex;

    public IndexUpdater(FileIndex fileIndex) {
        this.fileIndex = fileIndex;
    }

    @Override
    public void onFilesAddedEvent(Path filePath) throws NotHandledEventException {
        try {
            Files.walkFileTree(filePath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path dir, BasicFileAttributes attrs) throws IOException {
                    fileIndex.addFile(dir.toFile().getAbsolutePath());
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new NotHandledEventException("files adding failed due to IO error, details: " + e.getMessage());
        }
    }

    @Override
    public void onFilesRemovedEvent(Path filePath) {
        if (Files.isDirectory(filePath)) {
            fileIndex.removeDirectory(filePath.toFile().getAbsolutePath());
        } else {
            fileIndex.removeFileIteratingAll(filePath.toFile().getAbsolutePath());
        }
    }

    @Override
    public void onFilesModifiedEvent(Path filePath) throws NotHandledEventException {
        try {
            fileIndex.handleFileModification(filePath.toFile().getAbsolutePath());
        } catch (InconsistentIndexException e) {
            throw new NotHandledEventException("index has become inconsistent while modification");
        }
    }
}
