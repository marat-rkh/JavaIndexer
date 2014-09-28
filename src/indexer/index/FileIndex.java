package indexer.index;

import indexer.exceptions.InconsistentIndexException;
import indexer.tokenizer.Token;

import java.io.IOException;
import java.util.List;

/**
 * Created by mrx on 26.09.14.
 */
public interface FileIndex {
    public List<String> search(Token tokenToFind);

    public boolean addFile(String filePath);
    public boolean removeFileReadingDisk(String filePath);
    public void removeFileIteratingAll(String filePath);
    public boolean handleFileModification(String filePath) throws InconsistentIndexException;

    public boolean containsFile(String filePath);

    public void removeDirectory(String dirPath);
}
