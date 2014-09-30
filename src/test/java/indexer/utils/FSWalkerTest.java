package indexer.utils;

import indexer.TmpFsCreator;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FSWalkerTest extends TmpFsCreator {

    @Test
    public void testWalkFile() throws Exception {
        FSWalker fsWalker = new FSWalker();
        fsWalker.walk(file1.toPath());
        assertTrue(fsWalker.isFinished());
        assertEquals(1, fsWalker.getCollectedFiles().size());
    }

    @Test
    public void testWalkOneDir() throws Exception {
        FSWalker fsWalker = new FSWalker();
        fsWalker.walk(dir2.toPath());
        Thread.sleep(5000);
        assertTrue(fsWalker.isFinished());
        assertEquals(1, fsWalker.getCollectedFiles().size());
    }

    @Test
    public void testWalkDirs() throws Exception {
        FSWalker fsWalker = new FSWalker();
        fsWalker.walk(tempFolder.getRoot().toPath());
        Thread.sleep(5000);
        assertTrue(fsWalker.isFinished());
        assertEquals(5, fsWalker.getCollectedFiles().size());
    }
}