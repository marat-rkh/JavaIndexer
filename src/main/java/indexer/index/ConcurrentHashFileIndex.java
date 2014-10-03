package indexer.index;

import indexer.exceptions.InconsistentIndexException;
import indexer.tokenizer.Token;
import indexer.tokenizer.Tokenizer;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Concurrent version of HashFileIndex. Supports multiple readers (search and contains queries) and
 * one writer (add, remove and modify queries) at a time
 *
 * @see indexer.index.HashFileIndex
 */
public class ConcurrentHashFileIndex implements FileIndex {
    private final HashFileIndex index;
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = readWriteLock.readLock();
    private final Lock writeLock = readWriteLock.writeLock();

    public ConcurrentHashFileIndex(Tokenizer tokenizer) {
        this.index = new HashFileIndex(tokenizer);
    }

    @Override
    public List<String> search(Token tokenToFind) {
        readLock.lock();
        try {
            return index.search(tokenToFind);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean addFile(String filePath) {
        writeLock.lock();
        try {
            return index.addFile(filePath);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void addFiles(List<String> filesPaths) {
        writeLock.lock();
        try {
            index.addFiles(filesPaths);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void removeFile(String filePath) {
        writeLock.lock();
        try {
            index.removeFile(filePath);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void forceRemoves() {
        writeLock.lock();
        try {
            index.forceRemoves();
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean handleFileModification(String filePath) throws InconsistentIndexException {
        writeLock.lock();
        try {
            return index.handleFileModification(filePath);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean containsFile(String filePath) {
        readLock.lock();
        try {
            return index.containsFile(filePath);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void removeDirectory(String dirPath) {
        writeLock.lock();
        try {
            index.removeDirectory(dirPath);
        } finally {
            writeLock.unlock();
        }
    }
}