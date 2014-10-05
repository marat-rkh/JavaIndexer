package indexer;

import indexer.exceptions.InconsistentIndexException;
import indexer.exceptions.IndexClosedException;
import indexer.exceptions.NotHandledEventException;
import indexer.fsmonitor.FSMonitorLifecycleHandler;
import indexer.fsmonitor.FSMonitorsManager;
import indexer.fsmonitor.IndexMonitorHandler;
import indexer.handler.IndexEventsHandler;
import indexer.handler.IndexUpdater;
import indexer.index.ConcurrentHashFileIndex;
import indexer.index.FileIndex;
import indexer.tokenizer.Token;
import indexer.tokenizer.Tokenizer;
import indexer.utils.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Filesystem indexer based on ConcurrentHashFileIndex. Supports adding and removing files and
 * directories in index, searching files containing some token. Moreover, if some events in
 * filesystem happen (files or directories are added, removed or modified), index will be updated
 * appropriately using FSMonitorsManager.
 * FSIndexer is thread safe - supports multiple readers (search and contains queries) and
 * one writer (add and remove queries) at a time.
 *
 * @see indexer.index.ConcurrentHashFileIndex
 * @see indexer.fsmonitor.FSMonitorsManager
 */
public class FSIndexer implements AutoCloseable {
    private final FileIndex fileIndex;
    private final IndexEventsHandler indexEventsHandler;
    private final FSMonitorLifecycleHandler fsMonitorLifecycleHandler;
    private final FSMonitorsManager monitorsManager;

    private boolean isClosed = false;

    private final int MONITOR_RESTARTS_NUMBER = 3;

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = readWriteLock.readLock();
    private final Lock writeLock = readWriteLock.writeLock();

    public FSIndexer(FileIndex fileIndex, IndexEventsHandler indexEventsHandler,
                     FSMonitorLifecycleHandler fsMonitorLifecycleHandler, Logger logger) {
        this.fileIndex = fileIndex;
        this.indexEventsHandler = indexEventsHandler;
        this.fsMonitorLifecycleHandler = fsMonitorLifecycleHandler;
        this.monitorsManager = new FSMonitorsManager(indexEventsHandler, fsMonitorLifecycleHandler, logger);
    }

    /**
     * Searches all files in index containing {@code tokenToFind}
     *
     * @param tokenToFind token to search
     * @return            files containing passed token or empty list (if no such files in index)
     * @throws IndexClosedException if method is called after FSIndexer has been closed
     * @throws InconsistentIndexException if method is called after filesystem updating errors have been occurred
     */
    public List<String> search(Token tokenToFind) throws IndexClosedException, InconsistentIndexException {
        readLock.lock();
        try {
            checkState();
            return fileIndex.search(tokenToFind);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Adds file or directory to index
     *
     * @param filePath
     * @throws IndexClosedException if method is called after FSIndexer has been closed
     * @throws InconsistentIndexException if method is called after filesystem updating errors have been occurred
     * @throws IOException if IO errors occurred while adding
     */
    public void add(String filePath) throws IndexClosedException, InconsistentIndexException, IOException {
        readLock.lock();
        try {
            checkState();
            Path path = Paths.get(filePath);
            try {
                indexEventsHandler.onFilesAddedEvent(path);
            } catch (NotHandledEventException e) {
                throw new IOException("IO errors occurred while adding, details: " + e.getMessage());
            }
            if(!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                path = path.getParent();
            }
            monitorsManager.addMonitor(path, MONITOR_RESTARTS_NUMBER);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Removes file or directory form index
     *
     * @param filePath
     * @throws IndexClosedException if method is called after FSIndexer has been closed
     * @throws InconsistentIndexException if method is called after filesystem updating errors have been occurred
     * @throws IOException if IO errors occurred while removing
     */
    public void remove(String filePath) throws IndexClosedException, InconsistentIndexException, IOException {
        writeLock.lock();
        try {
            checkState();
            Path path = Paths.get(filePath);
            try {
                indexEventsHandler.onFilesRemovedEvent(path);
            } catch (NotHandledEventException e) {
                throw new IOException("IO errors occurred while removing, details: " + e.getMessage());
            }
            if(!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                path = path.getParent();
            }
            monitorsManager.removeMonitor(path);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Checks if specified file is in index
     *
     * @param filePath file to check
     * @return         {@code true} if file is in index, {@code false} otherwise
     * @throws IndexClosedException if method is called after FSIndexer has been closed
     * @throws InconsistentIndexException if method is called after filesystem updating errors have been occurred
     */
    public boolean containsFile(String filePath) throws IndexClosedException, InconsistentIndexException {
        readLock.lock();
        try {
            checkState();
            return fileIndex.containsFile(filePath);
        } finally {
            readLock.unlock();
        }
    }

    public IndexEventsHandler getIndexEventsHandler() {
        return indexEventsHandler;
    }

    public FileIndex getFileIndex() {
        return fileIndex;
    }

    public FSMonitorLifecycleHandler getFsMonitorLifecycleHandler() {
        return fsMonitorLifecycleHandler;
    }

    public void close() throws IOException {
        writeLock.lock();
        try {
            monitorsManager.stopAllMonitors();
            isClosed = true;
        } finally {
            writeLock.unlock();
        }
    }

    private void checkState() throws IndexClosedException, InconsistentIndexException {
        if(isClosed) {
            throw new IndexClosedException();
        } else if(fsMonitorLifecycleHandler.isMonitorDown() || monitorsManager.isErrorOccurred()) {
            throw new InconsistentIndexException("Index has become inconsistent due to filesystem updating errors");
        }
    }
}
