package indexer.index;

import indexer.exceptions.InconsistentIndexException;
import indexer.tokenizer.Token;
import indexer.tokenizer.Tokenizer;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by mrx on 27.09.14.
 */
public class ConcurrentHashFileIndex implements FileIndex {
    private final HashFileIndex index;
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    public ConcurrentHashFileIndex(Tokenizer tokenizer) {
        this.index = new HashFileIndex(tokenizer);
    }

    @Override
    public List<String> search(Token tokenToFind) {
        readWriteLock.readLock().lock();
        try {
            return index.search(tokenToFind);
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    @Override
    public boolean addFile(String filePath) {
        readWriteLock.writeLock().lock();
        try {
            index.addFile(filePath);
        } finally {
            readWriteLock.writeLock().unlock();
        }
        return false;
    }

    @Override
    public void removeFileIteratingAll(String filePath) {
        readWriteLock.writeLock().lock();
        try {
            index.removeFileIteratingAll(filePath);
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    @Override
    public boolean removeFileReadingDisk(String filePath) {
        readWriteLock.writeLock().lock();
        try {
            index.removeFileReadingDisk(filePath);
        } finally {
            readWriteLock.writeLock().unlock();
        }
        return false;
    }

    @Override
    public boolean handleFileModification(String filePath) throws InconsistentIndexException {
        readWriteLock.writeLock().lock();
        try {
            index.handleFileModification(filePath);
        } finally {
            readWriteLock.writeLock().unlock();
        }
        return false;
    }

    @Override
    public boolean containsFile(String filePath) {
        readWriteLock.readLock().lock();
        try {
            return index.containsFile(filePath);
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    @Override
    public void removeDirectory(String dirPath) {
        readWriteLock.writeLock().lock();
        try {
            index.removeDirectory(dirPath);
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }
}