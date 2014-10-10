package indexer.index;

import indexer.exceptions.InconsistentIndexException;
import indexer.tokenizer.Token;
import indexer.tokenizer.Tokenizer;
import indexer.utils.EncodedFile;
import indexer.utils.FileEntry;
import indexer.utils.PathUtils;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * FileIndex interface implementation based on HashMap with lazy removes
 *
 * @see indexer.index.FileIndex
 */
public class HashFileIndex implements FileIndex {
    private final Map<Token, ArrayList<Long>> tokenFilesMap = new ConcurrentHashMap<>();
    private final Map<Long, FileEntry> idFileMap = new HashMap<>();
    private final Map<String, Long> fileIdMap = new HashMap<>();

    private final AtomicLong lastAddedFileId = new AtomicLong(-1);
    private long version = 0;

    private final Tokenizer tokenizer;

    public HashFileIndex(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    /**
     * Searches files in index containing specified token. While collecting resulting list of file
     * performs postponed file removes
     *
     * @param tokenToFind token to find in index
     * @return            list of files containing specified token
     */
    @Override
    public List<String> search(Token tokenToFind) {
        if(tokenToFind != null) {
            ArrayList<Long> filesForToken = tokenFilesMap.get(tokenToFind);
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
     * @param encodedFile file's path and charset containing descriptor
     * @return         {@code true} if file has been added or already presents in index.
     *                 {@code false} is returned if IO problems occurred while reading file from disk
     */
    @Override
    public boolean addFile(EncodedFile encodedFile) {
        if(new File(encodedFile.getFilePath()).canRead()) {
            if (!containsFile(encodedFile.getFilePath())) {
                List<Token> tokens = readTokens(encodedFile);
                if (tokens == null) {
                    return false;
                }
                lastAddedFileId.incrementAndGet();
                int putTokens = putTokensToMap(tokens);
                idFileMap.put(lastAddedFileId.get(), new FileEntry(encodedFile.getFilePath(), putTokens));
                fileIdMap.put(encodedFile.getFilePath(), lastAddedFileId.get());
            }
            return true;
        }
        return false;
    }

    /**
     * Adds multiple files in index
     *
     * @param files file's path and charset containing descriptors
     */
    @Override
    public void addFiles(List<EncodedFile> files) {
        for(EncodedFile file : files) {
            addFile(file);
        }
    }

    /**
     * Lazy removes file from index. Real remove will be performed within search method calls or if method
     * forceRemoves called
     *
     * @param filePath file to remove from index
     */
    @Override
    public void removeFile(String filePath) {
        if(containsFile(filePath)) {
            Long fileId = fileIdMap.get(filePath);
            idFileMap.get(fileId).setRemoved();
            fileIdMap.remove(filePath);
        }
    }

    /**
     * Performs all postponed removes
     */
    @Override
    public void forceRemoves() {
        Iterator<Map.Entry<Token, ArrayList<Long>>> tokenEntryIt = tokenFilesMap.entrySet().iterator();
        while (tokenEntryIt.hasNext()) {
            Map.Entry<Token, ArrayList<Long>> tokenEntry = tokenEntryIt.next();
            doPostponedRemoves(tokenEntry.getValue());
            if(tokenEntry.getValue().size() == 0) {
                tokenEntryIt.remove();
            }
        }
    }

    /**
     * Updates file in index by marking old version as 'removed' and adding new version from disk.
     *
     * @param encodedFile file's path and charset containing descriptor
     * @return         {@code true} if file has been updated or no such file in index.
     *                 {@code false} is returned if file can not be read from disk
     * @throws InconsistentIndexException if file has been removed and than IO errors occurred while adding it again
     */
    @Override
    public boolean handleFileModification(EncodedFile encodedFile) throws InconsistentIndexException {
        if(new File(encodedFile.getFilePath()).canRead()) {
            if(containsFile(encodedFile.getFilePath())) {
                removeFile(encodedFile.getFilePath());
                if(!addFile(encodedFile)) {
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

    @Override
    public void removeDirectory(String dirPath) {
        Path path = Paths.get(dirPath);
        Iterator<Map.Entry<String, Long>> it = fileIdMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> entry = it.next();
            Path filePath = Paths.get(entry.getKey());
            Long id = entry.getValue();
            if(!PathUtils.pathsAreEqual(path, filePath) && PathUtils.firstPathIsParent(path, filePath)) {
                idFileMap.get(id).setRemoved();
                it.remove();
            }
        }
    }

//    public class IndexIterator implements Iterator<String> {
//        private final List<Long> ids;
//        private int pos = 0;
//
//        private IndexIterator(List<Long> fileIds) {
//            this.ids = fileIds;
//            this.files = idFileMap;
//            this.onCreateVersion = onCreateVersion;
//        }
//
//        public String next() {
//            if(version != onCreateVersion) {
//                isInvalid = true;
//                return null;
//            }
//            if(pos == ids.size()) {
//                return null;
//            }
//            int
//        }
//
//        public boolean isInvalid() {
//            return isInvalid;
//        }
//    }

    private void doPostponedRemoves(ArrayList<Long> tokenFiles) {
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
        if(fileEntry.getTokensCounter() <= 0) {
            fileIdMap.remove(fileEntry.getFilePath());
            idFileMap.remove(fileId);
        }
        ++version;
    }

    private List<String> getPaths(ArrayList<Long> filesForToken) {
        List<String> paths = new ArrayList<>(filesForToken.size());
        for(Long id : filesForToken) {
            paths.add(idFileMap.get(id).getFilePath());
        }
        return paths;
    }

    private List<Token> readTokens(EncodedFile encodedFile) {
        List<Token> tokens;
        try (Reader reader = new BufferedReader(new InputStreamReader(
                             new FileInputStream(encodedFile.getFilePath()), encodedFile.getCharset()))) {
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
        ArrayList<Long> filesId = tokenFilesMap.get(token);
        if(filesId == null) {
            filesId = new ArrayList<>();
            filesId.add(newId);
            tokenFilesMap.put(token, filesId);
            return true;
        } else if(filesId.get(filesId.size() - 1) != newId) {
            filesId.add(newId);
            return true;
        }
        return false;
    }
}