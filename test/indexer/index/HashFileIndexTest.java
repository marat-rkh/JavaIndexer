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

public class HashFileIndexTest {
    private Tokenizer tokenizer = new WordsTokenizer();

    //todo: tmp is not cleared
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testAddAndSearch() throws Exception {
        FileIndex hashFileIndex = new HashFileIndex(tokenizer);
        File loremFile = createLoremTestFile();
        hashFileIndex.addFile(loremFile.getAbsolutePath());
        assertTrue(hashFileIndex.search(new Word("Lorem")).size() == 1);
        assertTrue(hashFileIndex.search(new Word("in")).size() == 1);
        assertTrue(hashFileIndex.search(new Word("notInFile")).size() == 0);
    }

    @Test
    public void testContainsFile() throws Exception {
        FileIndex hashFileIndex = new HashFileIndex(tokenizer);
        File loremFile = createLoremTestFile();
        String loremFilePath = loremFile.getAbsolutePath();
        hashFileIndex.addFile(loremFilePath);
        assertTrue(hashFileIndex.containsFile(loremFilePath));
    }

    @Test
    public void testRemoveFileReadingDisk() throws Exception {
        FileIndex hashFileIndex = new HashFileIndex(tokenizer);
        File loremFile = createLoremTestFile();
        String loremFilePath = loremFile.getAbsolutePath();
        hashFileIndex.addFile(loremFilePath);
        assertTrue(hashFileIndex.containsFile(loremFilePath));
        assertTrue(hashFileIndex.search(new Word("Lorem")).size() == 1);
        hashFileIndex.removeFile(loremFilePath);
        assertTrue(!hashFileIndex.containsFile(loremFilePath));
        assertTrue(hashFileIndex.search(new Word("Lorem")).size() == 0);
    }

    @Test
    public void testRemoveFileIteratingAll() throws Exception {
        FileIndex hashFileIndex = new HashFileIndex(tokenizer);
        File loremFile = createLoremTestFile();
        String loremFilePath = loremFile.getAbsolutePath();
        hashFileIndex.addFile(loremFilePath);
        assertTrue(hashFileIndex.containsFile(loremFilePath));
        assertTrue(hashFileIndex.search(new Word("Lorem")).size() == 1);
        if(!loremFile.delete()) {
            fail("Manual deleting problem occurred");
        }
        hashFileIndex.removeFile(loremFilePath);
        assertTrue(!hashFileIndex.containsFile(loremFilePath));
        assertTrue(hashFileIndex.search(new Word("Lorem")).size() == 0);
    }

    @Test
    public void testHandleFileModification() throws Exception {
        FileIndex hashFileIndex = new HashFileIndex(tokenizer);
        File loremFile = createLoremTestFile();
        String loremFilePath = loremFile.getAbsolutePath();
        hashFileIndex.addFile(loremFilePath);
        assertTrue(hashFileIndex.containsFile(loremFilePath));
        assertTrue(hashFileIndex.search(new Word("Lorem")).size() == 1);
        appendTextToFile(loremFile, "appendix");
        hashFileIndex.handleFileModification(loremFilePath);
        assertTrue(hashFileIndex.containsFile(loremFilePath));
        assertTrue(hashFileIndex.search(new Word("Lorem")).size() == 1);
        assertTrue(hashFileIndex.search(new Word("appendix")).size() == 1);
    }

    private File createLoremTestFile() {
        final String LOREM_TEXT = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do\n" +
                "eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, " +
                "quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. " +
                "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur.\n" +
                "Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";
        return createTmpFile("LoremTest", LOREM_TEXT);
    }

    private File createOneSentenceTestFile() {
        final String TEXT = "English sentence here";
        return createTmpFile("OneSentence", TEXT);
    }

    private File createTmpFile(String fileName, String content) {
        try {
            File tmpFile = tempFolder.newFile(fileName);
            return appendTextToFile(tmpFile, content) ? tmpFile : null;
        } catch (IOException e) {
            return null;
        }
    }

    private boolean appendTextToFile(File file, String contentToAppend) {
        try {
            FileWriter fileWriter = new FileWriter(file.getAbsoluteFile());
            fileWriter.write(contentToAppend);
            fileWriter.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}