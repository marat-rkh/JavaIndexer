package indexer.index;

import indexer.tokenizer.Token;
import indexer.tokenizer.Tokenizer;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by mrx on 27.09.14.
 */
public class ConcurrentHashIndex implements Index {
    private final HashIndex index;
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    public ConcurrentHashIndex(Tokenizer tokenizer) {
        this.index = new HashIndex(tokenizer);
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
    public void addFile(String filePath) throws IOException {
        readWriteLock.writeLock().lock();
        try {
            index.addFile(filePath);
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    @Override
    public void removeFile(String filePath) throws IOException {
        readWriteLock.writeLock().lock();
        try {
            index.removeFile(filePath);
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    @Override
    public void handleFileModification(String filePath) throws IOException {
        readWriteLock.writeLock().lock();
        try {
            index.handleFileModification(filePath);
        } finally {
            readWriteLock.writeLock().unlock();
        }
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
}