package indexer.utils;

import indexer.TmpFsCreator;
import org.junit.Test;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FSWalkerTest extends TmpFsCreator {

    @Test
    public void testWalkFile() throws Exception {
        BlockingQueue<File> filesQueue = new LinkedBlockingQueue<>();
        FSWalker fsWalker = new FSWalker(filesQueue);
        fsWalker.startWalking(file1.toPath());
        assertTrue(fsWalker.isFinished());
        assertEquals(2, filesQueue.size());
    }

    @Test
    public void testWalkOneDir() throws Exception {
        BlockingQueue<File> filesQueue = new LinkedBlockingQueue<>();
        FSWalker fsWalker = new FSWalker(filesQueue);
        fsWalker.startWalking(dir2.toPath());
        Thread.sleep(5000);
        assertTrue(fsWalker.isFinished());
        assertEquals(2, filesQueue.size());
    }

    @Test
    public void testWalkDirs() throws Exception {
        BlockingQueue<File> filesQueue = new LinkedBlockingQueue<>();
        FSWalker fsWalker = new FSWalker(filesQueue);
        fsWalker.startWalking(tempFolder.getRoot().toPath());
        Thread.sleep(5000);
        assertTrue(fsWalker.isFinished());
        assertEquals(6, filesQueue.size());
    }
}