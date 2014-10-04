package indexer;

import indexer.tokenizer.WordsTokenizer;
import indexer.utils.ConsoleLogger;
import indexer.utils.ConsoleReadWriter;
import indexer.utils.ReadWriter;

import java.io.IOException;

/**
 * Created by mrx on 27.09.14.
 */
public class Main {
    public static void main(String[] args) throws IOException {
        try (ReadWriter readWriter = new ConsoleReadWriter();
             FSIndexer fsIndexer = Indexers.newSimpleFsIndexer(new WordsTokenizer(), new ConsoleLogger(readWriter))) {
            ExampleRepl repl = new ExampleRepl(fsIndexer, readWriter);
            repl.start();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}