package indexer.fsmonitor;

import indexer.handler.IndexEventsHandler;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by mrx on 27.09.14.
 */
public class FSMonitorsManager {
    private final Map<Path, FSMonitor> monitors = new HashMap<Path, FSMonitor>();
    private final IndexEventsHandler indexEventsHandler;

    public FSMonitorsManager(IndexEventsHandler indexEventsHandler) {
        this.indexEventsHandler = indexEventsHandler;
    }

    public boolean addMonitor(Path directory) {
        if(addingIsNeeded(directory)) {
            try {
                final FSMonitor newMonitor = new SingleDirMonitor(indexEventsHandler, directory);
                monitors.put(directory, newMonitor);
                Thread monitorThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        newMonitor.startMonitoring();
                    }
                });
                monitorThread.start();
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    public boolean removeMonitor(Path directory) {
        return monitors.remove(directory) != null;
    }

    public void stopAllMonitors() {
        for(FSMonitor fsMonitor : monitors.values()) {
            fsMonitor.stopMonitoring();
        }
    }

    private boolean addingIsNeeded(Path dirToAdd) {
        Iterator<Map.Entry<Path, FSMonitor>> it = monitors.entrySet().iterator();
        while(it.hasNext()) {
            Path currentPath = it.next().getKey();
            if(pathsAreEqual(currentPath, dirToAdd) || firstPathIsParent(currentPath, dirToAdd)) {
                return false;
            } else if(firstPathIsParent(dirToAdd, currentPath)) {
                it.remove();
            }
        }
        return true;
    }

    private boolean pathsAreEqual(Path fst, Path snd) { return fst.startsWith(snd) && snd.startsWith(fst); }
    private boolean firstPathIsParent(Path fst, Path snd) { return snd.startsWith(fst); }
}
