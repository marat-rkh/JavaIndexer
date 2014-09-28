package indexer;

import indexer.exceptions.IndexClosedException;
import indexer.fsmonitor.FSMonitorsManager;
import indexer.handler.IndexEventsHandler;
import indexer.handler.IndexUpdater;
import indexer.index.ConcurrentHashFileIndex;
import indexer.index.FileIndex;
import indexer.tokenizer.Token;
import indexer.tokenizer.Tokenizer;

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

    private boolean isClosed = false;

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    public FSIndexer(Tokenizer tokenizer) {
        fileIndex = new ConcurrentHashFileIndex(tokenizer);
        indexUpdater = new IndexUpdater(fileIndex);
        monitorsManager = new FSMonitorsManager(indexUpdater);
    }

    public List<String> search(Token tokenToFind) throws IndexClosedException {
        readWriteLock.readLock().lock();
        try {
            throwIfClosed();
            return fileIndex.search(tokenToFind);
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    public void add(String filePath) throws IndexClosedException {
        readWriteLock.writeLock().lock();
        try {
            throwIfClosed();
            Path path = Paths.get(filePath);
            indexUpdater.onFilesAddedEvent(path);
            if(!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                path = path.getParent();
            }
            monitorsManager.addMonitor(path);
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    public void remove(String filePath) throws IndexClosedException {
        readWriteLock.writeLock().lock();
        try {
            throwIfClosed();
            Path path = Paths.get(filePath);
            indexUpdater.onFilesRemovedEvent(path);
            if(!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                path = path.getParent();
            }
            monitorsManager.removeMonitor(path);
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    public boolean containsFile(String filePath) throws IndexClosedException {
        readWriteLock.readLock().lock();
        try {
            throwIfClosed();
            return fileIndex.containsFile(filePath);
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    public void close() {
        readWriteLock.writeLock().lock();
        try {
            monitorsManager.stopAllMonitors();
            isClosed = true;
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    private void throwIfClosed() throws IndexClosedException {
        if(isClosed) {
            throw new IndexClosedException();
        }
    }
}
