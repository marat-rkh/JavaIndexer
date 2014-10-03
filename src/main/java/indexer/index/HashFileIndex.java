package indexer.index;

import indexer.exceptions.InconsistentIndexException;
import indexer.tokenizer.Token;
import indexer.tokenizer.Tokenizer;
import indexer.utils.FileEntry;
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
    private final Map<Token, LinkedList<Long>> tokenFilesMap = new HashMap<>();
    private final Map<Long, FileEntry> idFileMap = new HashMap<>();
    private final Map<String, Long> fileIdMap = new HashMap<>();

    private final AtomicLong lastAddedFileId = new AtomicLong(-1);

    private final Tokenizer tokenizer;

    public HashFileIndex(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    @Override
    public List<String> search(Token tokenToFind) {
        if(tokenToFind != null) {
            LinkedList<Long> filesForToken = tokenFilesMap.get(tokenToFind);
            if (filesForToken != null) {
                doPostponedRemoves(filesForToken);
                if(filesForToken.size() == 0) {
                    tokenFilesMap.remove(tokenToFind);
                    return new LinkedList<>();
                } else {
                    return getPaths(filesForToken);
                }
            }
        }
        return new ArrayList<>();
    }

    /**
     * Adds file to index. File's content is retrieved using tokenizer provided in constructor. If specified file
     * is already in index, it will not be updated.
     *
     * @param filePath path of file to be added
     * @return         {@code true} if file has been added or already presents in index.
     *                 {@code false} is returned if IO problems occurred while reading file from disk
     */
    @Override
    public boolean addFile(String filePath) {
        if(new File(filePath).canRead()) {
            if (!containsFile(filePath)) {
                List<Token> tokens = readTokens(filePath);
                if (tokens == null) {
                    return false;
                }
                lastAddedFileId.incrementAndGet();
                int putTokens = putTokensToMap(tokens);
                idFileMap.put(lastAddedFileId.get(), new FileEntry(filePath, putTokens));
                fileIdMap.put(filePath, lastAddedFileId.get());
            }
            return true;
        }
        return false;
    }

    @Override
    public void addFiles(List<String> filesPaths) {
        for(String filePath : filesPaths) {
            addFile(filePath);
        }
    }

    @Override
    public void removeFile(String filePath) {
        if(containsFile(filePath)) {
            Long fileId = fileIdMap.get(filePath);
            idFileMap.get(fileId).setRemoved();
            fileIdMap.remove(filePath);
        }
    }

    @Override
    public void forceRemoves() {
        Iterator<Map.Entry<Token, LinkedList<Long>>> tokenEntryIt = tokenFilesMap.entrySet().iterator();
        while (tokenEntryIt.hasNext()) {
            Map.Entry<Token, LinkedList<Long>> tokenEntry = tokenEntryIt.next();
            doPostponedRemoves(tokenEntry.getValue());
            if(tokenEntry.getValue().size() == 0) {
                tokenEntryIt.remove();
            }
        }
    }

    /**
     * Updates file in index by marking old version as 'removed' and adding new version from disk.
     *
     * @param filePath path of file to be updated
     * @return         {@code true} if file has been updated or no such file in index.
     *                 {@code false} is returned if file can not be read from disk
     * @throws InconsistentIndexException if file has been removed and than IO errors occurred while adding it again
     */
    @Override
    public boolean handleFileModification(String filePath) throws InconsistentIndexException {
        if(new File(filePath).canRead()) {
            if(containsFile(filePath)) {
                removeFile(filePath);
                if(!addFile(filePath)) {
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
     * Removes specified directory from index additionally performing all postponed file removes
     *
     * @param dirPath directory to remove
     */
    @Override
    public void removeDirectory(String dirPath) {
        Path path = Paths.get(dirPath);
        Iterator<Map.Entry<Token, LinkedList<Long>>> it = tokenFilesMap.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry<Token, LinkedList<Long>> entry = it.next();
            doPostponedRemoves(entry.getValue());
            removeChildren(path, entry.getValue());
            if(entry.getValue().isEmpty()) {
                it.remove();
            }
        }
        removeFromFileIdMaps(path);
    }

    private void doPostponedRemoves(LinkedList<Long> tokenFiles) {
        Iterator<Long> filesIdIt = tokenFiles.iterator();
        while (filesIdIt.hasNext()) {
            Long fileId = filesIdIt.next();
            FileEntry fileEntry = idFileMap.get(fileId);
            if(fileEntry.isRemoved()) {
                doPostponedRemove(filesIdIt, fileId, fileEntry);
            }
        }
    }

    private void doPostponedRemove(Iterator<Long> filesIdIt, Long fileId, FileEntry fileEntry) {
        filesIdIt.remove();
        fileEntry.decreaseTokensCounter();
        if(fileEntry.getTokensCounter() == 0) {
            fileIdMap.remove(fileEntry.getFilePath());
            idFileMap.remove(fileId);
        }
    }

    private List<String> getPaths(LinkedList<Long> filesForToken) {
        List<String> paths = new ArrayList<>();
        for(Long id : filesForToken) {
            paths.add(idFileMap.get(id).getFilePath());
        }
        return paths;
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

    private int putTokensToMap(List<Token> tokens) {
        int putTokens = 0;
        for(Token tokenToAdd : tokens) {
            if(putInMap(tokenToAdd, lastAddedFileId.get())) {
                putTokens += 1;
            }
        }
        return putTokens;
    }

    private boolean putInMap(Token token, long newId) {
        LinkedList<Long> filesId = tokenFilesMap.get(token);
        if(filesId == null) {
            filesId = new LinkedList<>();
            filesId.add(newId);
            tokenFilesMap.put(token, filesId);
            return true;
        } else if(filesId.getLast() != newId) {
            filesId.add(newId);
            return true;
        }
        return false;
    }

//    private <K, E> boolean putInMap(Map<K, LinkedList<E>> map, K key, E newLinkedListEntry) {
//        LinkedList<E> currentValue = map.get(key);
//        if(currentValue == null) {
//            currentValue = new LinkedList<>();
//            currentValue.add(newLinkedListEntry);
//            map.put(key, currentValue);
//            return true;
//        } else if(currentValue.getLast() != newLinkedListEntry) {
//            currentValue.add(newLinkedListEntry);
//            return true;
//        }
//        return false;
//    }

    private void removeChildren(Path parentPath, LinkedList<Long> filesId) {
        Iterator<Long> it = filesId.iterator();
        while(it.hasNext()) {
            Path filePath = Paths.get(idFileMap.get(it.next()).getFilePath());
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