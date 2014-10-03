package indexer.utils;

/**
 * Created by mrx on 03.10.14.
 */
public class FileEntry {
    private final String filePath;
    private boolean removed = false;
    private int tokensCounter;

    public FileEntry(String filePath, int wordsNumber) {
        this.filePath = filePath;
        this.tokensCounter = wordsNumber;
    }

    public boolean isRemoved() {
        return removed;
    }

    public void setRemoved() {
        this.removed = true;
    }

    public void decreaseTokensCounter() {
        tokensCounter -= 1;
    }

    public int getTokensCounter() {
        return tokensCounter;
    }

    public String getFilePath() {
        return filePath;
    }
}
