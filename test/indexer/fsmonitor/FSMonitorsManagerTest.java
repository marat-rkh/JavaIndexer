package indexer.fsmonitor;

import indexer.TmpFsCreator;
import org.junit.Test;

import static org.junit.Assert.*;

public class FSMonitorsManagerTest extends TmpFsCreator{
    @Test
    public void testAddRemoveMonitors() throws Exception {
        TestEventsHandler handler = new TestEventsHandler();
        FSMonitorsManager manager = new FSMonitorsManager(handler);
        assertTrue(manager.addMonitor(dir1.toPath()));
        assertTrue(manager.addMonitor(dir1.toPath()));
        assertTrue(manager.addMonitor(tempFolder.getRoot().toPath()));
        assertTrue(manager.addMonitor(dir2.toPath()));
        assertFalse(manager.removeMonitor(dir1.toPath()));
        assertFalse(manager.removeMonitor(dir2.toPath()));
        assertTrue(manager.removeMonitor(tempFolder.getRoot().toPath()));
        manager.stopAllMonitors();
    }
}