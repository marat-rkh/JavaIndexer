package indexer.utils;

import java.nio.charset.Charset;

/**
 * Created by mrx on 09.10.14.
 */
public class EncodedFile {
    private final String filePath;
    private final Charset charset;

    public EncodedFile(String filePath, Charset charset) {
        this.filePath = filePath;
        this.charset = charset;
    }

    public EncodedFile(String filePath) {
        this(filePath, Charset.defaultCharset());
    }

    public String getFilePath() {
        return filePath;
    }

    public Charset getCharset() {
        return charset;
    }
}
