import java.nio.file.Path;
import java.util.List;

/**
 * Created by mrx on 26.09.14.
 */
public interface Index {
    public List<String> search(String word);

    public void addFile(Path filePath);
    public void removeFile(Path filePath);
    public void handleFileModification(Path filePath);
}
