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
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * IndexEventsHandler interface implementation
 *
 * @see indexer.handler.IndexEventsHandler
 */
public class IndexUpdater implements IndexEventsHandler {
    private final FileIndex fileIndex;
    private final int ADD_FILE_CACHE_SIZE = 10000;
    private final String TEXT_MIME_PREFIX = "text/";

    public IndexUpdater(FileIndex fileIndex) {
        this.fileIndex = fileIndex;
    }

    @Override
    public void onFilesAddedEvent(Path filePath) throws NotHandledEventException {
        final List<String> cache = new LinkedList<>();
        final ExecutorService addersPool = Executors.newFixedThreadPool(1);
        try {
            Files.walkFileTree(filePath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String mimeType = Files.probeContentType(file);
                    if(mimeType.startsWith(TEXT_MIME_PREFIX)) {
                        cache.add(file.toFile().getAbsolutePath());
                        if (cache.size() > ADD_FILE_CACHE_SIZE) {
                            addersPool.execute(new Adder(new LinkedList<String>(cache)));
                            cache.clear();
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new NotHandledEventException("files adding failed due to IO error, details: " + e.getMessage());
        }
        fileIndex.addFiles(cache);
        addersPool.shutdown();
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

    private class Adder implements Runnable {
        private List<String> filesList;

        public Adder(List<String> filesList) {
            this.filesList = filesList;
        }

        @Override
        public void run() {
            fileIndex.addFiles(filesList);
        }
    }
}
