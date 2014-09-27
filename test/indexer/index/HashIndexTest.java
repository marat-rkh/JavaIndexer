package indexer.index;

import indexer.tokenizer.Tokenizer;
import indexer.tokenizer.Word;
import indexer.tokenizer.WordsTokenizer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.junit.Assert.*;

public class HashIndexTest {
    private Tokenizer tokenizer = new WordsTokenizer();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testAddAndSearch() throws Exception {
        Index hashIndex = new HashIndex(tokenizer);
        String loremFilePath = createLoremTestFile();
        hashIndex.addFile(loremFilePath);
        assertTrue(hashIndex.search(new Word("Lorem")).size() == 1);
        assertTrue(hashIndex.search(new Word("in")).size() == 1);
        assertTrue(hashIndex.search(new Word("notInFile")).size() == 0);
    }

    @Test
    public void testContainsFile() throws Exception {
        Index hashIndex = new HashIndex(tokenizer);
        String loremFilePath = createLoremTestFile();
        hashIndex.addFile(loremFilePath);
        assertTrue(hashIndex.containsFile(loremFilePath));
    }

    private String createLoremTestFile() {
        final String LOREM_TEXT = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do\n" +
                "eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, " +
                "quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. " +
                "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur.\n" +
                "Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";
        try {
            File loremTest = tempFolder.newFile("LoremTest");
            FileWriter fileWriter = new FileWriter(loremTest.getAbsoluteFile());
            fileWriter.write(LOREM_TEXT);
            fileWriter.close();
            return loremTest.getAbsolutePath();
        } catch (IOException e) {
            return null;
        }
    }
}