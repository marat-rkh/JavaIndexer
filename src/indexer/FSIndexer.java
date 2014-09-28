package indexer;

import indexer.exceptions.InconsistentIndexException;
import indexer.exceptions.IndexClosedException;
import indexer.exceptions.NotHandledEventException;
import indexer.fsmonitor.FSMonitorsManager;
import indexer.fsmonitor.IndexMonitorHandler;
import indexer.handler.IndexEventsHandler;
import indexer.handler.IndexUpdater;
import indexer.index.ConcurrentHashFileIndex;
import indexer.index.FileIndex;
import indexer.tokenizer.Token;
import indexer.tokenizer.Tokenizer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by mrx on 27.09.14.
 */
public class FSIndexer implements AutoCloseable {
    private final FileIndex fileIndex;
    private final IndexEventsHandler indexUpdater;
    private final FSMonitorsManager monitorsManager;
    private final IndexMonitorHandler indexMonitorHandler;

    private boolean isClosed = false;

    private final int MONITOR_RESTARTS_NUMBER = 3;

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    public FSIndexer(Tokenizer tokenizer) {
        fileIndex = new ConcurrentHashFileIndex(tokenizer);
        indexUpdater = new IndexUpdater(fileIndex);
        indexMonitorHandler = new IndexMonitorHandler(indexUpdater);
        monitorsManager = new FSMonitorsManager(indexUpdater, indexMonitorHandler);
    }

    public List<String> search(Token tokenToFind) throws IndexClosedException, InconsistentIndexException {
        readWriteLock.readLock().lock();
        try {
            checkState();
            return fileIndex.search(tokenToFind);
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    public void add(String filePath) throws IndexClosedException, InconsistentIndexException, IOException {
        readWriteLock.writeLock().lock();
        try {
            checkState();
            Path path = Paths.get(filePath);
            try {
                indexUpdater.onFilesAddedEvent(path);
            } catch (NotHandledEventException e) {
                throw new IOException("IO errors occurred while adding, details: " + e.getMessage());
            }
            if(!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                path = path.getParent();
            }
            monitorsManager.addMonitor(path, MONITOR_RESTARTS_NUMBER);
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    public void remove(String filePath) throws IndexClosedException, InconsistentIndexException, IOException {
        readWriteLock.writeLock().lock();
        try {
            checkState();
            Path path = Paths.get(filePath);
            try {
                indexUpdater.onFilesRemovedEvent(path);
            } catch (NotHandledEventException e) {
                throw new IOException("IO errors occurred while removing, details: " + e.getMessage());
            }
            if(!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                path = path.getParent();
            }
            monitorsManager.removeMonitor(path);
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    public boolean containsFile(String filePath) throws IndexClosedException, InconsistentIndexException {
        readWriteLock.readLock().lock();
        try {
            checkState();
            return fileIndex.containsFile(filePath);
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    public void close() throws IOException {
        readWriteLock.writeLock().lock();
        try {
            monitorsManager.stopAllMonitors();
            isClosed = true;
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    private void checkState() throws IndexClosedException, InconsistentIndexException {
        if(isClosed) {
            throw new IndexClosedException();
        } else if(indexMonitorHandler.isSomeMonitorDown() || monitorsManager.isErrorOccurred()) {
            throw new InconsistentIndexException("Index has become inconsistent due to filesystem updating errors");
        }
    }
}
