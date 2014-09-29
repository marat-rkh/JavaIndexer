package indexer.fsmonitor;

import indexer.TmpFsCreator;
import indexer.exceptions.NotHandledEventException;
import org.junit.Test;

import java.nio.file.Path;

import static org.junit.Assert.*;

public class FSMonitorsManagerTest extends TmpFsCreator{
    @Test
    public void testAddRemoveMonitors() throws Exception {
        TestEventsHandler handler = new TestEventsHandler();
        FSMonitorsManager manager = new FSMonitorsManager(handler, new TestMonitorHandler(), null);
        assertTrue(manager.addMonitor(dir1.toPath(), 0));
        assertTrue(manager.addMonitor(dir1.toPath(), 0));
        assertTrue(manager.addMonitor(tempFolder.getRoot().toPath(), 0));
        assertTrue(manager.addMonitor(dir2.toPath(), 0));
        assertFalse(manager.removeMonitor(dir1.toPath()));
        assertFalse(manager.removeMonitor(dir2.toPath()));
        assertTrue(manager.removeMonitor(tempFolder.getRoot().toPath()));
        manager.stopAllMonitors();
    }

    private class TestMonitorHandler implements FSMonitorLifecycleHandler {
        @Override
        public void onMonitorRestart(Path monitorsDirectory) throws NotHandledEventException {}
        @Override
        public void onMonitorDown(Path monitorsDirectory) throws NotHandledEventException {}
    }
}