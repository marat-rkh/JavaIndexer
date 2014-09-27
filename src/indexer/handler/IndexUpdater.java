package indexer.handler;

import indexer.index.FileIndex;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Created by mrx on 27.09.14.
 */
public class IndexUpdater implements IndexEventsHandler {
    private final FileIndex fileIndex;

    public IndexUpdater(FileIndex fileIndex) {
        this.fileIndex = fileIndex;
    }

    @Override
    public void onFilesAddedEvent(Path filePath) {
        try {
            Files.walkFileTree(filePath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (!Files.isDirectory(dir)) {
                        fileIndex.addFile(dir.toFile().getAbsolutePath());
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onFilesRemovedEvent(Path filePath) {
        try {
            Files.walkFileTree(filePath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (!Files.isDirectory(dir)) {
                        fileIndex.removeFile(dir.toFile().getAbsolutePath());
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onFilesModifiedEvent(Path filePath) {
        try {
            fileIndex.handleFileModification(filePath.toFile().getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
