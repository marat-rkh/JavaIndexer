package indexer.handler;

import indexer.encoding.DetectionResult;
import indexer.encoding.EncodingDetector;
import indexer.exceptions.InconsistentIndexException;
import indexer.exceptions.NotHandledEventException;
import indexer.index.FileIndex;
import indexer.utils.EncodedFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * IndexEventsHandler interface implementation
 *
 * @see indexer.handler.IndexEventsHandler
 */
public class IndexUpdater implements IndexEventsHandler {
    private final FileIndex fileIndex;
    private final int ADD_FILE_CACHE_SIZE = 1000;

    public IndexUpdater(FileIndex fileIndex) {
        this.fileIndex = fileIndex;
    }

    @Override
    public void onFilesAddedEvent(Path filePath) throws NotHandledEventException {
        final List<EncodedFile> cache = new LinkedList<>();
        final ExecutorService addersPool = Executors.newFixedThreadPool(1);
//        if(filePath.toFile().isFile()) {
//            cache.add(filePath.toFile().getAbsolutePath());
//        } else {
            try {
                Files.walkFileTree(filePath, new AdderFileVisitor(addersPool, cache));
            } catch (IOException e) {
                throw new NotHandledEventException("files adding failed due to IO error, details: " + e.getMessage());
            }
//        }
        fileIndex.addFiles(cache);
        waitAddersToFinish(addersPool);
    }

    @Override
    public void onFilesRemovedEvent(Path filePath) {
        if(fileIndex.containsFile(filePath.toFile().getAbsolutePath())) {
            fileIndex.removeFile(filePath.toFile().getAbsolutePath());
        } else {
            fileIndex.removeDirectory(filePath.toFile().getAbsolutePath());
        }
    }

    @Override
    public void onFilesModifiedEvent(Path filePath) throws NotHandledEventException {
        try {
            File file = filePath.toFile();
            DetectionResult result = EncodingDetector.standardDetector().detect(file.getAbsolutePath());
            if(result != null) {
                EncodedFile encodedFile = new EncodedFile(file.getAbsolutePath(), result.getCharset());
                fileIndex.handleFileModification(encodedFile);
            } else {
                fileIndex.removeFile(file.getAbsolutePath());
            }
        } catch (InconsistentIndexException e) {
            throw new NotHandledEventException("index has become inconsistent while modification");
        } catch (IOException e) {
            throw new NotHandledEventException("modification event hasn't been handled due to IO errors");
        }
    }

    private void waitAddersToFinish(ExecutorService addersPool) {
        addersPool.shutdown();
        try {
            while (!addersPool.awaitTermination(1, TimeUnit.SECONDS)) { }
        } catch (InterruptedException e) {
            // suppressed
        }
    }

    private class AdderFileVisitor extends SimpleFileVisitor<Path> {
        private final EncodingDetector detector = EncodingDetector.standardDetector();
        private final ExecutorService addersPool;
        private final List<EncodedFile> cache;

        private AdderFileVisitor(ExecutorService addersPool, List<EncodedFile> cache) {
            this.addersPool = addersPool;
            this.cache = cache;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            DetectionResult result = detector.detect(file.toFile().getAbsolutePath());
            if (result != null) {
                // todo: use detected charset to read!
                EncodedFile encodedFile = new EncodedFile(file.toFile().getAbsolutePath(), result.getCharset());
                cache.add(encodedFile);
                if (cache.size() > ADD_FILE_CACHE_SIZE) {
                    addersPool.execute(new Adder(new LinkedList<>(cache)));
                    cache.clear();
                }
            }
            return FileVisitResult.CONTINUE;
        }
    }

    private class Adder implements Runnable {
        private List<EncodedFile> filesList;

        public Adder(List<EncodedFile> filesList) {
            this.filesList = filesList;
        }

        @Override
        public void run() {
            fileIndex.addFiles(filesList);
        }
    }
}
