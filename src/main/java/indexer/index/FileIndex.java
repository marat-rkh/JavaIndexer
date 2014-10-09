package indexer.index;

import indexer.exceptions.InconsistentIndexException;
import indexer.tokenizer.Token;
import indexer.utils.EncodedFile;

import java.nio.charset.Charset;
import java.util.List;

/**
 * Interface for files index that support adding, removing, updating file if it was modified
 * and searching files containing specified token.
 */
public interface FileIndex {
    public List<String> search(Token tokenToFind);

    public boolean addFile(EncodedFile encodedFile);
    public void addFiles(List<EncodedFile> files);
    public void removeFile(String filePath);
    public void forceRemoves();
    public boolean handleFileModification(EncodedFile encodedFile) throws InconsistentIndexException;

    public boolean containsFile(String filePath);

    public void removeDirectory(String dirPath);
}
