package indexer.handler;

import indexer.TmpFsCreator;
import indexer.index.FileIndex;
import indexer.index.HashFileIndex;
import indexer.tokenizer.Tokenizer;
import indexer.tokenizer.Word;
import indexer.tokenizer.WordsTokenizer;
import org.junit.Test;

import java.nio.file.Paths;

import static org.junit.Assert.*;

public class IndexUpdaterTest extends TmpFsCreator {
    private Tokenizer tokenizer = new WordsTokenizer();

    @Test
    public void testOnOneFileAddedEvent() throws Exception {
        FileIndex hashFileIndex = new HashFileIndex(tokenizer);
        IndexEventsHandler handler = new IndexUpdater(hashFileIndex);

        handler.onFilesAddedEvent(Paths.get(dir1SubFile1.getAbsolutePath()));
        assertTrue(hashFileIndex.containsFile(dir1SubFile1.getAbsolutePath()));

        handler.onFilesAddedEvent(Paths.get(file1.getAbsolutePath()));
        assertTrue(hashFileIndex.containsFile(file1.getAbsolutePath()));
        assertEquals(1, hashFileIndex.search(new Word("file1")).size());

        handler.onFilesAddedEvent(Paths.get(file1.getAbsolutePath()));
        assertTrue(hashFileIndex.containsFile(file1.getAbsolutePath()));
        assertEquals(1, hashFileIndex.search(new Word("file1")).size());

        handler.onFilesAddedEvent(Paths.get(dir2SubFile1.getAbsolutePath()));
        assertEquals(1, hashFileIndex.search(new Word("file1")).size());
        assertEquals(1, hashFileIndex.search(new Word("amet,")).size());
    }

    @Test
    public void testOnFilesAddedEvent() throws Exception {
        FileIndex hashFileIndex = new HashFileIndex(tokenizer);
        IndexEventsHandler handler = new IndexUpdater(hashFileIndex);

        handler.onFilesAddedEvent(Paths.get(dir1.getAbsolutePath()));
        assertTrue(hashFileIndex.containsFile(dir1SubFile1.getAbsolutePath()));

        handler.onFilesAddedEvent(Paths.get(dir2.getAbsolutePath()));
        assertTrue(hashFileIndex.containsFile(dir2SubFile1.getAbsolutePath()));
        assertEquals(1, hashFileIndex.search(new Word("ipsum")).size());

        handler.onFilesAddedEvent(Paths.get(tempFolder.getRoot().getAbsolutePath()));
        assertTrue(hashFileIndex.containsFile(file1.getAbsolutePath()));
        assertTrue(hashFileIndex.containsFile(file2.getAbsolutePath()));
        assertTrue(hashFileIndex.containsFile(file3.getAbsolutePath()));
        assertTrue(hashFileIndex.containsFile(dir1SubFile1.getAbsolutePath()));
        assertTrue(hashFileIndex.containsFile(dir2SubFile1.getAbsolutePath()));
        assertEquals(3, hashFileIndex.search(new Word("content")).size());
        assertEquals(1, hashFileIndex.search(new Word("file1")).size());
        assertEquals(1, hashFileIndex.search(new Word("file2")).size());
        assertEquals(1, hashFileIndex.search(new Word("file3")).size());
        assertEquals(1, hashFileIndex.search(new Word("ipsum")).size());
    }

    @Test
    public void testOnOneFileRemovedEvent() throws Exception {
        FileIndex hashFileIndex = new HashFileIndex(tokenizer);
        IndexEventsHandler handler = new IndexUpdater(hashFileIndex);
        handler.onFilesAddedEvent(Paths.get(tempFolder.getRoot().getAbsolutePath()));

        handler.onFilesRemovedEvent(Paths.get(dir1SubFile1.getAbsolutePath()));
        assertTrue(hashFileIndex.containsFile(file1.getAbsolutePath()));
        assertTrue(hashFileIndex.containsFile(file2.getAbsolutePath()));
        assertTrue(hashFileIndex.containsFile(file3.getAbsolutePath()));
        assertFalse(hashFileIndex.containsFile(dir1SubFile1.getAbsolutePath()));
        assertTrue(hashFileIndex.containsFile(dir2SubFile1.getAbsolutePath()));

        handler.onFilesRemovedEvent(Paths.get(dir1SubFile1.getAbsolutePath()));
        assertTrue(hashFileIndex.containsFile(file1.getAbsolutePath()));
        assertTrue(hashFileIndex.containsFile(file2.getAbsolutePath()));
        assertTrue(hashFileIndex.containsFile(file3.getAbsolutePath()));
        assertFalse(hashFileIndex.containsFile(dir1SubFile1.getAbsolutePath()));
        assertTrue(hashFileIndex.containsFile(dir2SubFile1.getAbsolutePath()));

        handler.onFilesRemovedEvent(Paths.get(file1.getAbsolutePath()));
        assertFalse(hashFileIndex.containsFile(file1.getAbsolutePath()));
        assertTrue(hashFileIndex.containsFile(file2.getAbsolutePath()));
        assertTrue(hashFileIndex.containsFile(file3.getAbsolutePath()));
        assertFalse(hashFileIndex.containsFile(dir1SubFile1.getAbsolutePath()));
        assertTrue(hashFileIndex.containsFile(dir2SubFile1.getAbsolutePath()));
    }

    @Test
    public void testOnFilesRemovedEvent() throws Exception {
        FileIndex hashFileIndex = new HashFileIndex(tokenizer);
        IndexEventsHandler handler = new IndexUpdater(hashFileIndex);
        handler.onFilesAddedEvent(Paths.get(tempFolder.getRoot().getAbsolutePath()));

        handler.onFilesRemovedEvent(Paths.get(dir1.getAbsolutePath()));
        assertTrue(hashFileIndex.containsFile(file1.getAbsolutePath()));
        assertTrue(hashFileIndex.containsFile(file2.getAbsolutePath()));
        assertTrue(hashFileIndex.containsFile(file3.getAbsolutePath()));
        assertFalse(hashFileIndex.containsFile(dir1SubFile1.getAbsolutePath()));
        assertTrue(hashFileIndex.containsFile(dir2SubFile1.getAbsolutePath()));
        assertEquals(1, hashFileIndex.search(new Word("file2")).size());
        assertEquals(1, hashFileIndex.search(new Word("ipsum")).size());

        handler.onFilesRemovedEvent(Paths.get(dir2.getAbsolutePath()));
        assertTrue(hashFileIndex.containsFile(file1.getAbsolutePath()));
        assertTrue(hashFileIndex.containsFile(file2.getAbsolutePath()));
        assertTrue(hashFileIndex.containsFile(file3.getAbsolutePath()));
        assertFalse(hashFileIndex.containsFile(dir1SubFile1.getAbsolutePath()));
        assertFalse(hashFileIndex.containsFile(dir2SubFile1.getAbsolutePath()));
        assertEquals(1, hashFileIndex.search(new Word("file2")).size());
        assertEquals(0, hashFileIndex.search(new Word("ipsum")).size());

        handler.onFilesRemovedEvent(Paths.get(tempFolder.getRoot().getAbsolutePath()));
        assertFalse(hashFileIndex.containsFile(file1.getAbsolutePath()));
        assertFalse(hashFileIndex.containsFile(file2.getAbsolutePath()));
        assertFalse(hashFileIndex.containsFile(file3.getAbsolutePath()));
        assertFalse(hashFileIndex.containsFile(dir1SubFile1.getAbsolutePath()));
        assertFalse(hashFileIndex.containsFile(dir2SubFile1.getAbsolutePath()));
        assertEquals(0, hashFileIndex.search(new Word("file2")).size());
        assertEquals(0, hashFileIndex.search(new Word("ipsum")).size());
    }

    @Test
    public void testOnFilesModifiedEvent() throws Exception {
        FileIndex hashFileIndex = new HashFileIndex(tokenizer);
        IndexEventsHandler handler = new IndexUpdater(hashFileIndex);
        handler.onFilesAddedEvent(Paths.get(file1.getAbsolutePath()));
        handler.onFilesAddedEvent(Paths.get(file2.getAbsolutePath()));
        handler.onFilesAddedEvent(Paths.get(file3.getAbsolutePath()));
        assertEquals(1, hashFileIndex.search(new Word("file1")).size());
        assertEquals(3, hashFileIndex.search(new Word("content")).size());

        rewriteFileWithText(file1, "nothing more here");
        handler.onFilesModifiedEvent(Paths.get(file1.getAbsolutePath()));

        assertEquals(0, hashFileIndex.search(new Word("file1")).size());
        assertEquals(2, hashFileIndex.search(new Word("content")).size());
        assertEquals(1, hashFileIndex.search(new Word("nothing")).size());
        assertEquals(1, hashFileIndex.search(new Word("more")).size());
        assertEquals(1, hashFileIndex.search(new Word("here")).size());
    }
}