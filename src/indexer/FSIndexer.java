package indexer;

import indexer.fsmonitor.FSMonitorsManager;
import indexer.handler.IndexEventsHandler;
import indexer.handler.IndexUpdater;
import indexer.index.ConcurrentHashFileIndex;
import indexer.index.FileIndex;
import indexer.tokenizer.Token;
import indexer.tokenizer.Tokenizer;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Created by mrx on 27.09.14.
 */
public class FSIndexer {
    private final FileIndex fileIndex;
    private final IndexEventsHandler indexUpdater;
    private final FSMonitorsManager monitorsManager;

    private boolean isClosed = false;

    public FSIndexer(Tokenizer tokenizer) {
        fileIndex = new ConcurrentHashFileIndex(tokenizer);
        indexUpdater = new IndexUpdater(fileIndex);
        monitorsManager = new FSMonitorsManager(indexUpdater);
    }

    public List<String> search(Token tokenToFind) throws Exception {
        throwIfClosed();
        return fileIndex.search(tokenToFind);
    }

    public void add(String filePath) throws Exception {
        throwIfClosed();
        Path path = Paths.get(filePath);
        indexUpdater.onFilesAddedEvent(path);
        if(!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            path = path.getParent();
        }
        monitorsManager.addMonitor(path);
    }

    public void remove(String filePath) throws Exception {
        throwIfClosed();
        Path path = Paths.get(filePath);
        indexUpdater.onFilesRemovedEvent(path);
        if(!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            path = path.getParent();
        }
        monitorsManager.removeMonitor(path);
    }

    public boolean containsFile(String filePath) throws Exception {
        throwIfClosed();
        return fileIndex.containsFile(filePath);
    }

    public void close() {
        monitorsManager.stopAllMonitors();
        isClosed = true;
    }

    private void throwIfClosed() throws Exception {
        if(isClosed) {
            throw new Exception("FSIndexer is closed");
        }
    }
}
