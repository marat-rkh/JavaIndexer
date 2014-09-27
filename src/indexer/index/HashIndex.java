package indexer.index;

import indexer.tokenizer.Token;
import indexer.tokenizer.Tokenizer;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by mrx on 27.09.14.
 */
public class HashIndex implements Index {
    private final Map<Token, HashSet<Long>> tokenFilesMap = new HashMap<Token, HashSet<Long>>();
    private final Map<Long, String> fileIdMap = new HashMap<Long, String>();
    private final Map<String, HashSet<Token>> fileTokensMap = new HashMap<String, HashSet<Token>>();

    private final AtomicLong lastAddedFileId = new AtomicLong(-1);

    private final Tokenizer tokenizer;

    public HashIndex(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    @Override
    public List<String> search(Token tokenToFind) {
        if(tokenToFind != null) {
            Set<Long> filesForToken = tokenFilesMap.get(tokenToFind);
            if (filesForToken != null) {
                List<String> filePathsForToken = new LinkedList<String>();
                for(Long fileId : filesForToken) {
                    filePathsForToken.add(fileIdMap.get(fileId));
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
        if(!fileIdMap.containsKey(filePath)) {
            Reader reader = new FileReader(filePath);
            List<Token> tokens = tokenizer.tokenize(reader);
            reader.close();
            fileIdMap.put(lastAddedFileId.incrementAndGet(), filePath);
            putTokensToMap(tokens);
        }
    }

    @Override
    public void removeFile(String filePath) {

    }

    @Override
    public void handleFileModification(String filePath) {

    }

    @Override
    public boolean containsFile(String filePath) {
        return fileTokensMap.containsKey(filePath);
    }

    private void putTokensToMap(List<Token> tokens) {
        for(Token tokenToAdd : tokens) {
            HashSet<Long> value = tokenFilesMap.get(tokenToAdd);
            if(value == null) {
                value = new HashSet<Long>();
                value.add(lastAddedFileId.get());
                tokenFilesMap.put(tokenToAdd, value);
            } else {
                value.add(lastAddedFileId.get());
            }
        }
    }
}
