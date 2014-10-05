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

    private boolean useMimeTypes = true;
    private final Set<String> extensions = new HashSet<>();

    public IndexUpdater(FileIndex fileIndex) {
        this.fileIndex = fileIndex;
    }

    @Override
    public void onFilesAddedEvent(Path filePath) throws NotHandledEventException {
        final List<String> cache = new LinkedList<>();
        final ExecutorService addersPool = Executors.newFixedThreadPool(1);
        if(filePath.toFile().isFile()) {
            cache.add(filePath.toFile().getAbsolutePath());
        } else {
            try {
                Files.walkFileTree(filePath, new AdderFileVisitor(addersPool, cache));
            } catch (IOException e) {
                throw new NotHandledEventException("files adding failed due to IO error, details: " + e.getMessage());
            }
        }
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
            fileIndex.handleFileModification(filePath.toFile().getAbsolutePath());
        } catch (InconsistentIndexException e) {
            throw new NotHandledEventException("index has become inconsistent while modification");
        }
    }

    public void useMimeTypes() {
        useMimeTypes = true;
    }

    public void useExtensions() {
        useMimeTypes = false;
    }

    public void addExtensions(List<String> exts) {
        for(String e : exts) {
            if(!e.equals("")) {
                extensions.add(e);
            }
        }
    }

    public void removeExtensions(List<String> exts) {
        for(String e : exts) {
            extensions.remove(e);
        }
    }

    public Set<String> getCurrentExtensions() {
        return extensions;
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
        private final String TEXT_MIME_PREFIX = "text/";
        private final ExecutorService addersPool;
        private final List<String> cache;

        private AdderFileVisitor(ExecutorService addersPool, List<String> cache) {
            this.addersPool = addersPool;
            this.cache = cache;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            String ext = getExtension(file);
            if(useMimeTypes && mimeIsText(file) || ext != null && extensions.contains(ext)) {
                cache.add(file.toFile().getAbsolutePath());
                if (cache.size() > ADD_FILE_CACHE_SIZE) {
                    addersPool.execute(new Adder(new LinkedList<>(cache)));
                    cache.clear();
                }
            }
            return FileVisitResult.CONTINUE;
        }

        private boolean mimeIsText(Path file) throws IOException {
            String mimeType = Files.probeContentType(file);
            return mimeType != null && mimeType.startsWith(TEXT_MIME_PREFIX);
        }

        private String getExtension(Path file) {
            String filePath = file.toFile().getAbsolutePath().toLowerCase();
            int dotIndex = filePath.lastIndexOf(".");
            if(dotIndex != -1 && dotIndex != 0) {
                return filePath.substring(dotIndex + 1);
            }
            return null;
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
