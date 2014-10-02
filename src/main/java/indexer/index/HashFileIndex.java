package indexer.index;

import indexer.exceptions.InconsistentIndexException;
import indexer.tokenizer.Token;
import indexer.tokenizer.Tokenizer;
import indexer.utils.PathUtils;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * FileIndex interface implementation based on HashMap.
 *
 * @see indexer.index.FileIndex
 */
public class HashFileIndex implements FileIndex {
    private final Map<Token, HashSet<Long>> tokenFilesMap = new HashMap<Token, HashSet<Long>>();
    private final Map<Long, String> idFileMap = new HashMap<Long, String>();
    private final Map<String, Long> fileIdMap = new HashMap<String, Long>();

    private final AtomicLong lastAddedFileId = new AtomicLong(-1);

    private final Tokenizer tokenizer;

    public HashFileIndex(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    /**
     * Searches all files in index containing {@code tokenToFind}
     *
     * @param tokenToFind token to search
     * @return            files containing passed token or empty list (if no such files in index)
     */
    @Override
    public List<String> search(Token tokenToFind) {
        if(tokenToFind != null) {
            Set<Long> filesForToken = tokenFilesMap.get(tokenToFind);
            if (filesForToken != null) {
                List<String> filePathsForToken = new LinkedList<String>();
                for(Long fileId : filesForToken) {
                    filePathsForToken.add(idFileMap.get(fileId));
                }
                return filePathsForToken;
            }
        }
        return new LinkedList<String>();
    }

    /**
     * Adds file to index. File's content is retrieved using tokenizer provided in constructor. If specified file
     * is already in index, it will not be updated.
     *
     * @param filePath path of file to be added
     * @return         {@code true} if file has been added or already presents in index.
     *                 {@code false} is returned if IO errors occurred while reading file from disk
     */
    @Override
    public boolean addFile(String filePath) {
        if(!containsFile(filePath)) {
            List<Token> tokens = readTokens(filePath);
            if(tokens == null) {
                return false;
            }
            idFileMap.put(lastAddedFileId.incrementAndGet(), filePath);
            fileIdMap.put(filePath, lastAddedFileId.get());
            putTokensToMaps(tokens, filePath);
        }
        return true;
    }

    @Override
    public void addFiles(List<String> filesPaths) {
        for(String filePath : filesPaths) {
            addFile(filePath);
        }
    }

    /**
     * Removes file iterating while index. Common use case - file has been removed from disk and
     * removing from index is needed too.
     *
     * @param filePath path of file to be removed
     */
    @Override
    public void removeFileIteratingAll(String filePath) {
        if(containsFile(filePath)) {
            Long fileId = fileIdMap.get(filePath);
            removeIteratingAll(fileId);
            idFileMap.remove(fileId);
            fileIdMap.remove(filePath);
        }
    }

    /**
     * Removes file reading it's content from disk.
     *
     * @param filePath path of file to be removed
     * @return         {@code true} if file has been removed or no such file in index.
     *                 {@code false} is returned if IO errors occurred while reading file from disk
     */
    @Override
    public boolean removeFileReadingDisk(String filePath) {
        if(containsFile(filePath)) {
            Long fileId = fileIdMap.get(filePath);
            if(!removeReadingFromDisk(filePath, fileId)) {
                return false;
            }
            idFileMap.remove(fileId);
            fileIdMap.remove(filePath);
        }
        return true;
    }

    /**
     * Updates file in index by removing it from index and adding again.
     *
     * @param filePath path of file to be updated
     * @return         {@code true} if file has been removed or no such file in index.
     *                 {@code false} is returned if file can not be read from disk
     * @throws InconsistentIndexException if file has been removed and than IO errors occurred while adding it again
     */
    @Override
    public boolean handleFileModification(String filePath) throws InconsistentIndexException {
        if(new File(filePath).canRead()) {
            if(containsFile(filePath)) {
                removeFileIteratingAll(filePath);
                if (!addFile(filePath)) {
                    throw new InconsistentIndexException("IO error has made index inconsistent");
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Checks if specified file is in index
     *
     * @param filePath file to check
     * @return         {@code true} if file is in index, {@code false} otherwise
     */
    @Override
    public boolean containsFile(String filePath) {
        return fileIdMap.containsKey(filePath);
    }

    /**
     * Removes specified directory from index
     *
     * @param dirPath directory to remove
     */
    @Override
    public void removeDirectory(String dirPath) {
        Path path = Paths.get(dirPath);
        Iterator<Map.Entry<Token, HashSet<Long>>> it = tokenFilesMap.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry<Token, HashSet<Long>> entry = it.next();
            removeChildren(path, entry.getValue());
            if(entry.getValue().isEmpty()) {
                it.remove();
            }
        }
        removeFromFileIdMaps(path);
    }

    private List<Token> readTokens(String filePath) {
        List<Token> tokens;
        try (Reader reader = new BufferedReader(new FileReader(filePath))) {
            tokens = tokenizer.tokenize(reader);
        } catch (IOException e) {
            return null;
        }
        return tokens;
    }

    private void putTokensToMaps(List<Token> tokens, String filePath) {
        for(Token tokenToAdd : tokens) {
            putInMap(tokenFilesMap, tokenToAdd, lastAddedFileId.get());
        }
    }

    private <K, E> void putInMap(Map<K, HashSet<E>> map, K key, E newHashSetEntry) {
        HashSet<E> currentValue = map.get(key);
        if(currentValue == null) {
            currentValue = new HashSet<E>();
            currentValue.add(newHashSetEntry);
            map.put(key, currentValue);
        } else {
            currentValue.add(newHashSetEntry);
        }
    }

    private boolean removeReadingFromDisk(String filePath, Long fileId) {
        List<Token> tokens = readTokens(filePath);
        if(tokens != null) {
            for (Token tokenToRemove : tokens) {
                Set<Long> files = tokenFilesMap.get(tokenToRemove);
                if (files != null) {
                    files.remove(fileId);
                    if (files.isEmpty()) {
                        tokenFilesMap.remove(tokenToRemove);
                    }
                }
            }
            return true;
        }
        return false;
    }

    private void removeIteratingAll(Long fileId) {
        Iterator<Map.Entry<Token, HashSet<Long>>> it = tokenFilesMap.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry<Token, HashSet<Long>> entry = it.next();
            entry.getValue().remove(fileId);
            if(entry.getValue().isEmpty()) {
                it.remove();
            }
        }
    }

    private void removeChildren(Path parentPath, HashSet<Long> filesId) {
        Iterator<Long> it = filesId.iterator();
        while(it.hasNext()) {
            Path filePath = Paths.get(idFileMap.get(it.next()));
            if(!PathUtils.pathsAreEqual(parentPath, filePath) && PathUtils.firstPathIsParent(parentPath, filePath)) {
                it.remove();
            }
        }
    }

    private void removeFromFileIdMaps(Path path) {
        Iterator<Map.Entry<String, Long>> it = fileIdMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> entry = it.next();
            Path filePath = Paths.get(entry.getKey());
            Long id = entry.getValue();
            if(!PathUtils.pathsAreEqual(path, filePath) && PathUtils.firstPathIsParent(path, filePath)) {
                idFileMap.remove(id);
                it.remove();
            }
        }
    }
}