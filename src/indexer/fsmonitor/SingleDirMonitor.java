package indexer.fsmonitor;

import indexer.handler.IndexEventsHandler;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.nio.file.LinkOption;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * Created by mrx on 27.09.14.
 */
public class SingleDirMonitor implements FSMonitor {
    private final WatchService watchService;
    private final Map<WatchKey, Path> keyPathMap = new HashMap<WatchKey, Path>();
    private final IndexEventsHandler indexEventsHandler;

    public SingleDirMonitor(IndexEventsHandler indexEventsHandler, Path directory)
            throws Exception {
        this.watchService = FileSystems.getDefault().newWatchService();
        this.indexEventsHandler = indexEventsHandler;
        if(directory == null || !Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
            throw new Exception("passed directory is not correct: it is a file or null");
        }
        registerDirectory(directory);
    }

    @Override
    public void startMonitoring() {
        while (true) {
            WatchKey key;
            try {
                key = watchService.take();
            } catch (Exception e) {
                return;
            }
            Path registeredDir = keyPathMap.get(key);
            if (registeredDir == null) {
                continue;
            }
            handleEvents(key, registeredDir);
            if (!key.reset()) {
                keyPathMap.remove(key);
                if (keyPathMap.isEmpty()) {
                    break;
                }
            }
        }
    }

    @Override
    public void stopMonitoring() {
        try {
            watchService.close();
        } catch (IOException e) {

        }
    }

    private void registerDirectory(Path pathToTarget) throws IOException {
        Files.walkFileTree(pathToTarget, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                register(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void register(Path pathToTarget) throws IOException {
        WatchKey key = pathToTarget.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        keyPathMap.put(key, pathToTarget);
    }

    private void handleEvents(WatchKey key, Path registeredDir) {
        for (WatchEvent<?> event : key.pollEvents()) {
            WatchEvent.Kind kind = event.kind();
            if (kind == OVERFLOW) {
                // todo: index recreation may be needed
                System.out.println("DEBUG: event overflow");
                continue;
            }
            WatchEvent<Path> pathEvent = castWatchEvent(event);
            Path relativeChildPath = pathEvent.context();
            Path childPath = registeredDir.resolve(relativeChildPath);
            if(kind == ENTRY_CREATE) {
                handleCreateEvent(childPath);
            } else if(kind == ENTRY_DELETE) {
                handleDeleteEvent(childPath);
            } else if(kind == ENTRY_MODIFY) {
                handleModifyEvent(childPath);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T> WatchEvent<T> castWatchEvent(WatchEvent<?> event) {
        return (WatchEvent<T>)event;
    }

    private void handleCreateEvent(Path path) {
        try {
            if(Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                registerDirectory(path);
            }
            System.out.println("Created: " + path.toString());
            indexEventsHandler.onFilesAddedEvent(path);
        } catch (IOException x) {
            // registration failed
        }
    }
    private void handleDeleteEvent(Path path) {
        System.out.println("Deleted: " + path.toString());
        indexEventsHandler.onFilesRemovedEvent(path);
    }
    private void handleModifyEvent(Path path) {
        System.out.println("Modified: " + path.toString());
        indexEventsHandler.onFilesModifiedEvent(path);
    }
}
