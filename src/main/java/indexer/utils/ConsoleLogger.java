package indexer.utils;

import java.io.IOException;

/**
 * Created by mrx on 04.10.14.
 */
public class ConsoleLogger implements Logger {
    private final ReadWriter readWriter;

    public ConsoleLogger(ReadWriter readWriter) {
        this.readWriter = readWriter;
    }

    @Override
    public void log(String msg) throws IOException {
        readWriter.println("\n" + msg);
    }
}
