package indexer;

import indexer.tokenizer.WordsTokenizer;
import indexer.utils.ConsoleLogger;
import indexer.utils.ConsoleReadWriter;
import indexer.utils.FileReadWriter;
import indexer.utils.ReadWriter;

import java.io.IOException;

/**
 * Created by mrx on 27.09.14.
 */
public class Main {
    public static void main(String[] args) throws IOException {
        String commandsFile = args.length == 0 ? null : args[0];
        try (ReadWriter readWriter = getReadWriter(commandsFile);
             FSIndexer fsIndexer = Indexers.newSimpleFsIndexer(new WordsTokenizer(), new ConsoleLogger(readWriter))) {
            ExampleRepl repl = new ExampleRepl(fsIndexer, readWriter);
            repl.start();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static ReadWriter getReadWriter(String commandsFile) throws Exception {
        if(commandsFile == null) {
            return new ConsoleReadWriter();
        }
        return new FileReadWriter(commandsFile);
    }
}