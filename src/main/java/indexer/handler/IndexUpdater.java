package indexer.handler;

import indexer.exceptions.InconsistentIndexException;
import indexer.exceptions.NotHandledEventException;
import indexer.index.FileIndex;
import indexer.utils.FSWalker;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * IndexEventsHandler interface implementation
 *
 * @see indexer.handler.IndexEventsHandler
 */
public class IndexUpdater implements IndexEventsHandler {
    private final FileIndex fileIndex;
    private final int ADD_FILE_CACHE_SIZE = 50;

    public IndexUpdater(FileIndex fileIndex) {
        this.fileIndex = fileIndex;
    }

    @Override
    public void onFilesAddedEvent(Path filePath) throws NotHandledEventException {
        BlockingQueue<File> collectedFiles = new LinkedBlockingQueue<>();
        FSWalker fsWalker = new FSWalker(collectedFiles);
        fsWalker.startWalking(filePath);
        try {
            while (true) {
                List<File> cachedFiles = new LinkedList<>();
                while (cachedFiles.size() < ADD_FILE_CACHE_SIZE) {
                    File foundFile = collectedFiles.take();
                    if (foundFile.equals(FSWalker.FAKE_END_FILE)) {
                        addFilesFromList(cachedFiles);
                        return;
                    }
                    cachedFiles.add(foundFile);
                }
                addFilesFromList(cachedFiles);
            }
        } catch (InterruptedException e) {
            throw new NotHandledEventException("files adding has been interrupted");
        } finally {
            fsWalker.stop();
        }
    }

    @Override
    public void onFilesRemovedEvent(Path filePath) {
        if (Files.isDirectory(filePath)) {
            fileIndex.removeDirectory(filePath.toFile().getAbsolutePath());
        } else {
            fileIndex.removeFileIteratingAll(filePath.toFile().getAbsolutePath());
        }
    }

    @Override
    public void onFilesModifiedEvent(Path filePath) throws NotHandledEventException {
        try {
            fileIndex.handleFileModification(filePath.toFile().getAbsolutePath());
        } catch (InconsistentIndexException e) {
            throw new NotHandledEventException("index has become inconsistent while modification");
        }
    }

    private void addFilesFromList(List<File> filesList) {
        for(File f : filesList) {
            fileIndex.addFile(f.getAbsolutePath());
        }
    }
}
