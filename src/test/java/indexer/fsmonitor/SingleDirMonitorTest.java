package indexer.fsmonitor;

import indexer.TmpFsCreator;
import indexer.exceptions.NotHandledEventException;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

public class SingleDirMonitorTest extends TmpFsCreator {
    @Test
    public void testAddFileMonitoring() throws Exception {
        TestEventsHandler handler = new TestEventsHandler();
        FSMonitor monitor = new SingleDirMonitor(dir1.toPath(), handler);
        startMonitorThread(monitor);
        File addedFile = File.createTempFile("addedFile", "", dir1);
        Thread.sleep(10000);

        assertEquals(1, handler.addedPaths.size());
        assertEquals(0, handler.removedPaths.size());
        assertEquals(0, handler.modifiedPaths.size());
        assertEquals(addedFile.getAbsolutePath(), handler.addedPaths.get(0).getAbsolutePath());
        monitor.stopMonitoring();
    }

    @Test
    public void testAddDirMonitoring() throws Exception {
        TestEventsHandler handler = new TestEventsHandler();
        FSMonitor monitor = new SingleDirMonitor(tempFolder.getRoot().toPath(), handler);
        startMonitorThread(monitor);
        File addedDir = tempFolder.newFolder("addedDir");
        Thread.sleep(1000);

        assertEquals(1, handler.addedPaths.size());
        assertEquals(0, handler.removedPaths.size());
        assertEquals(0, handler.modifiedPaths.size());
        assertEquals(addedDir.getAbsolutePath(), handler.addedPaths.get(0).getAbsolutePath());
        monitor.stopMonitoring();
    }

    @Test
    public void testRemoveMonitoring() throws Exception {
        TestEventsHandler handler = new TestEventsHandler();
        FSMonitor monitor = new SingleDirMonitor(tempFolder.getRoot().toPath(), handler);
        startMonitorThread(monitor);
        if(!file1.delete() || !file2.delete()) {
            fail("manual file deleting failed");
        }
        Thread.sleep(1000);

        assertEquals(0, handler.addedPaths.size());
        assertEquals(2, handler.removedPaths.size());
        assertEquals(0, handler.modifiedPaths.size());
        monitor.stopMonitoring();
    }

    @Test
    public void testModifyMonitoring() throws Exception {
        TestEventsHandler handler = new TestEventsHandler();
        FSMonitor monitor = new SingleDirMonitor(tempFolder.getRoot().toPath(), handler);
        startMonitorThread(monitor);
        appendTextToFile(file1, "some text");
        Thread.sleep(1000);

        assertEquals(0, handler.addedPaths.size());
        assertEquals(0, handler.removedPaths.size());
        assertEquals(1, handler.modifiedPaths.size());
        assertEquals(file1.getAbsolutePath(), handler.modifiedPaths.get(0).getAbsolutePath());
        monitor.stopMonitoring();
    }

    private void startMonitorThread(final FSMonitor monitor) {
        Thread monitorThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    monitor.startMonitoring();
                } catch (NotHandledEventException e) {
                    fail("monitor not started");
                }
            }
        });
        monitorThread.start();
    }
}