package indexer.index;

import indexer.exceptions.InconsistentIndexException;
import indexer.tokenizer.Token;

import java.util.List;

/**
 * Interface for files index that support adding, removing, updating file if it was modified
 * and searching files containing specified token.
 */
public interface FileIndex {
    public List<String> search(Token tokenToFind);

    public boolean addFile(String filePath);
    public void addFiles(List<String> filesPaths);
    //    public boolean removeFileReadingDisk(String filePath);
    public void removeFile(String filePath);
    public void forceRemoves();
    public boolean handleFileModification(String filePath) throws InconsistentIndexException;

    public boolean containsFile(String filePath);

    public void removeDirectory(String dirPath);
}
