package indexer;

import indexer.fsmonitor.FSMonitorLifecycleHandler;
import indexer.fsmonitor.IndexMonitorHandler;
import indexer.handler.IndexEventsHandler;
import indexer.handler.IndexUpdater;
import indexer.index.ConcurrentHashFileIndex;
import indexer.index.FileIndex;
import indexer.tokenizer.Tokenizer;
import indexer.utils.Logger;

import java.io.OutputStream;

/**
 * Factory for FSIndexer class objects creation
 *
 * @see indexer.FSIndexer
 */
public class Indexers {
    public static FSIndexer newSimpleFsIndexer(Tokenizer tokenizer, Logger logger) {
        FileIndex fileIndex = new ConcurrentHashFileIndex(tokenizer);
        IndexEventsHandler indexUpdater = new IndexUpdater(fileIndex);
        FSMonitorLifecycleHandler fsMonitorLifecycleHandler = new IndexMonitorHandler(indexUpdater);
        return new FSIndexer(fileIndex, indexUpdater, fsMonitorLifecycleHandler, logger);
    }
}
