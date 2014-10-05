package indexer;

import indexer.fsmonitor.FSMonitorLifecycleHandler;
import indexer.handler.IndexEventsHandler;
import indexer.handler.IndexUpdater;
import indexer.index.FileIndex;
import indexer.utils.Logger;

import java.util.List;
import java.util.Set;

/**
 * Created by mrx on 05.10.14.
 */
public class ExtendedFSIndexer extends FSIndexer {

    public ExtendedFSIndexer(FileIndex fileIndex, IndexUpdater indexUpdater,
                             FSMonitorLifecycleHandler fsMonitorLifecycleHandler, Logger logger) {
        super(fileIndex, indexUpdater, fsMonitorLifecycleHandler, logger);
    }

    public void useMimeTypes() {
        ((IndexUpdater)getIndexEventsHandler()).useMimeTypes();
    }

    public void useExtensions() {
        ((IndexUpdater)getIndexEventsHandler()).useExtensions();
    }

    public void addExtensions(List<String> exts) {
        ((IndexUpdater)getIndexEventsHandler()).addExtensions(exts);
    }

    public void removeExtensions(List<String> exts) {
        ((IndexUpdater)getIndexEventsHandler()).removeExtensions(exts);
    }

    public Set<String> getCurrentExtensions() {
        return ((IndexUpdater)getIndexEventsHandler()).getCurrentExtensions();
    }
}
