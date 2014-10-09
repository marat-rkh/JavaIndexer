package indexer.index;

import indexer.TmpFsCreator;
import indexer.exceptions.InconsistentIndexException;
import indexer.tokenizer.Tokenizer;
import indexer.tokenizer.Word;
import indexer.tokenizer.WordsTokenizer;
import indexer.utils.EncodedFile;
import org.junit.Test;

import java.nio.charset.Charset;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class HashFileIndexTest extends TmpFsCreator {
    private Tokenizer tokenizer = new WordsTokenizer();

    @Test
    public void testAddAndSearchSimple() {
        FileIndex hashFileIndex = new HashFileIndex(tokenizer);
        hashFileIndex.addFile(new EncodedFile(file1.getAbsolutePath()));

        assertTrue(hashFileIndex.search(new Word("file1")).size() == 1);
        assertTrue(hashFileIndex.search(new Word("content")).size() == 1);
        assertTrue(hashFileIndex.search(new Word("notInFile")).size() == 0);
    }

    @Test
    public void testAddAndSearch() {
        FileIndex hashFileIndex = new HashFileIndex(tokenizer);
        hashFileIndex.addFile(new EncodedFile(file1.getAbsolutePath()));
        hashFileIndex.addFile(new EncodedFile(file2.getAbsolutePath()));
        hashFileIndex.addFile(new EncodedFile(file3.getAbsolutePath()));
        hashFileIndex.addFile(new EncodedFile(dir1SubFile1.getAbsolutePath()));
        hashFileIndex.addFile(new EncodedFile(dir2SubFile1.getAbsolutePath()));

        assertTrue(hashFileIndex.search(new Word("file1")).size() == 1);
        assertTrue(hashFileIndex.search(new Word("file2")).size() == 1);
        assertTrue(hashFileIndex.search(new Word("file3")).size() == 1);
        assertTrue(hashFileIndex.search(new Word("content")).size() == 3);
        assertTrue(hashFileIndex.search(new Word("notInFile")).size() == 0);
        assertTrue(hashFileIndex.search(new Word("Lorem")).size() == 1);
        assertTrue(hashFileIndex.search(new Word("ipsum")).size() == 1);
        assertTrue(hashFileIndex.search(new Word("dolor")).size() == 1);
        assertTrue(hashFileIndex.search(new Word("sit")).size() == 1);
        assertTrue(hashFileIndex.search(new Word("amet,")).size() == 1);
        assertTrue(hashFileIndex.search(new Word("consectetur")).size() == 1);
        assertTrue(hashFileIndex.search(new Word("adipiscing")).size() == 1);
        assertTrue(hashFileIndex.search(new Word("elit")).size() == 1);
    }

    @Test
    public void testContainsFile() {
        FileIndex hashFileIndex = new HashFileIndex(tokenizer);
        hashFileIndex.addFile(new EncodedFile(file1.getAbsolutePath()));

        assertTrue(hashFileIndex.containsFile(file1.getAbsolutePath()));
        assertTrue(!hashFileIndex.containsFile(file2.getAbsolutePath()));
    }

    @Test
    public void testRemoveFileReadingDisk() {
        FileIndex hashFileIndex = new HashFileIndex(tokenizer);
        hashFileIndex.addFile(new EncodedFile(dir2SubFile1.getAbsolutePath()));

        assertTrue(hashFileIndex.containsFile(dir2SubFile1.getAbsolutePath()));
        assertTrue(hashFileIndex.search(new Word("Lorem")).size() == 1);

        hashFileIndex.removeFile(dir2SubFile1.getAbsolutePath());

        assertTrue(!hashFileIndex.containsFile(dir2SubFile1.getAbsolutePath()));
        assertTrue(hashFileIndex.search(new Word("Lorem")).size() == 0);
    }

    @Test
    public void testHandleFileModificationAppend() throws InconsistentIndexException {
        FileIndex hashFileIndex = new HashFileIndex(tokenizer);
        hashFileIndex.addFile(new EncodedFile(dir2SubFile1.getAbsolutePath()));

        assertTrue(hashFileIndex.containsFile(dir2SubFile1.getAbsolutePath()));
        assertTrue(hashFileIndex.search(new Word("Lorem")).size() == 1);

        if(!appendTextToFile(dir2SubFile1, " appendix")) {
            fail("append text failed");
        }
        hashFileIndex.handleFileModification(new EncodedFile(dir2SubFile1.getAbsolutePath()));

        assertTrue(hashFileIndex.containsFile(dir2SubFile1.getAbsolutePath()));
        assertTrue(hashFileIndex.search(new Word("Lorem")).size() == 1);
        assertTrue(hashFileIndex.search(new Word("appendix")).size() == 1);
    }

    @Test
    public void testHandleFileModificationReplace() throws InconsistentIndexException {
        FileIndex hashFileIndex = new HashFileIndex(tokenizer);
        hashFileIndex.addFile(new EncodedFile(dir2SubFile1.getAbsolutePath()));

        assertTrue(hashFileIndex.containsFile(dir2SubFile1.getAbsolutePath()));
        assertTrue(hashFileIndex.search(new Word("Lorem")).size() == 1);

        if(!rewriteFileWithText(dir2SubFile1, "replacement")) {
            fail("rewrite text failed");
        }
        hashFileIndex.handleFileModification(new EncodedFile(dir2SubFile1.getAbsolutePath()));

        assertTrue(hashFileIndex.containsFile(dir2SubFile1.getAbsolutePath()));
        assertTrue(hashFileIndex.search(new Word("Lorem")).size() == 0);
        assertTrue(hashFileIndex.search(new Word("ipsum")).size() == 0);
        assertTrue(hashFileIndex.search(new Word("replacement")).size() == 1);
    }

    @Test
    public void testRemoveDirectory() {
        FileIndex hashFileIndex = new HashFileIndex(tokenizer);
        hashFileIndex.addFile(new EncodedFile(file1.getAbsolutePath()));
        hashFileIndex.addFile(new EncodedFile(file2.getAbsolutePath()));
        hashFileIndex.addFile(new EncodedFile(file3.getAbsolutePath()));
        hashFileIndex.addFile(new EncodedFile(dir1SubFile1.getAbsolutePath()));
        hashFileIndex.addFile(new EncodedFile(dir2SubFile1.getAbsolutePath()));
        hashFileIndex.removeDirectory(dir1.getAbsolutePath());

        assertTrue(hashFileIndex.containsFile(file1.getAbsolutePath()));
        assertTrue(hashFileIndex.containsFile(file2.getAbsolutePath()));
        assertTrue(hashFileIndex.containsFile(file3.getAbsolutePath()));
        assertFalse(hashFileIndex.containsFile(dir1SubFile1.getAbsolutePath()));
        assertTrue(hashFileIndex.containsFile(dir2SubFile1.getAbsolutePath()));

        hashFileIndex.removeDirectory(tempFolder.getRoot().getAbsolutePath());
        assertFalse(hashFileIndex.containsFile(file1.getAbsolutePath()));
        assertFalse(hashFileIndex.containsFile(file2.getAbsolutePath()));
        assertFalse(hashFileIndex.containsFile(file3.getAbsolutePath()));
        assertFalse(hashFileIndex.containsFile(dir1SubFile1.getAbsolutePath()));
        assertFalse(hashFileIndex.containsFile(dir2SubFile1.getAbsolutePath()));
    }
}