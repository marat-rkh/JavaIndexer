package indexer.index;

import indexer.tokenizer.Token;

import java.io.IOException;
import java.util.List;

/**
 * Created by mrx on 26.09.14.
 */
public interface Index {
    public List<String> search(Token tokenToFind);

    public void addFile(String filePath) throws IOException;
    public void removeFile(String filePath) throws IOException;
    public void handleFileModification(String filePath);

    public boolean containsFile(String filePath);
}
