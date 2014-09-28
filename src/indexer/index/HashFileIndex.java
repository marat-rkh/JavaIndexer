package indexer.index;

import indexer.tokenizer.Token;
import indexer.tokenizer.Tokenizer;
import indexer.utils.PathUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by mrx on 27.09.14.
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
     * Adds file's content to index. Content is retrieved using tokenizer provided in constructor. If specified file
     * is already in index, it will not be updated.
     *
     * @param filePath
     * @throws IOException
     */
    @Override
    public void addFile(String filePath) throws IOException {
        if(!containsFile(filePath)) {
            List<Token> tokens = readTokens(filePath);
            idFileMap.put(lastAddedFileId.incrementAndGet(), filePath);
            fileIdMap.put(filePath, lastAddedFileId.get());
            putTokensToMaps(tokens, filePath);
        }
    }

    @Override
    public void removeFileIteratingAll(String filePath) throws IOException {
        if(containsFile(filePath)) {
            Long fileId = fileIdMap.get(filePath);
            removeIteratingAll(fileId);
            idFileMap.remove(fileId);
            fileIdMap.remove(filePath);
        }
    }

    @Override
    public void removeFileReadingDisk(String filePath) throws IOException {
        if(containsFile(filePath)) {
            Long fileId = fileIdMap.get(filePath);
            removeReadingFromDisk(filePath, fileId);
            idFileMap.remove(fileId);
            fileIdMap.remove(filePath);
        }
    }

    @Override
    public void handleFileModification(String filePath) throws IOException {
        if(containsFile(filePath)) {
            removeFileIteratingAll(filePath);
            addFile(filePath);
        }
    }

    @Override
    public boolean containsFile(String filePath) {
        return fileIdMap.containsKey(filePath);
    }

    @Override
    public void removeDirectory(String dirPath) throws IOException {
        Path path = Paths.get(dirPath);
        Iterator<Map.Entry<Token, HashSet<Long>>> it = tokenFilesMap.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry<Token, HashSet<Long>> entry = it.next();
            removeChildren(path, entry.getValue());
            if(entry.getValue().isEmpty()) {
                it.remove();
            }
        }
    }

    private List<Token> readTokens(String filePath) throws IOException {
        Reader reader = new FileReader(filePath);
        List<Token> tokens = tokenizer.tokenize(reader);
        reader.close();
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

    private void removeReadingFromDisk(String filePath, Long fileId) throws IOException {
        List<Token> tokens = readTokens(filePath);
        for(Token tokenToRemove : tokens) {
            Set<Long> files = tokenFilesMap.get(tokenToRemove);
            if(files != null) {
                files.remove(fileId);
                if(files.isEmpty()) {
                    tokenFilesMap.remove(tokenToRemove);
                }
            }
        }
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
}