package indexer.fsmonitor;

import indexer.exceptions.NotHandledEventException;
import indexer.handler.IndexEventsHandler;
import indexer.utils.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * FSMonitor interface implementation for listening single directory content events and affecting index.
 * Class takes IndexEventsHandler as constructor argument and calls appropriate methods on
 * remove, add and modify events. Moreover, some output stream can be passed to trace events.
 * For listening WatchService class is used. Note, that this class in used only for listening
 * content of the folder not the folder itself.
 *
 * @see indexer.fsmonitor.FSMonitor
 * @see java.nio.file.WatchService
 * @see indexer.handler.IndexEventsHandler
 */
public class DirContentMonitor extends IndexUpdaterLogger implements FSMonitor {
    private final Path directory;
    private final WatchService watchService;
    private final Map<WatchKey, Path> keyPathMap = new HashMap<WatchKey, Path>();

    /**
     * Constructor with parameters. Note that it expects directory as the first argument and throws
     * NotDirectoryException if it is not (file or null)
     *
     * @param directory directory which events should be listened and handled
     * @param indexEventsHandler events handler
     * @param logger logger events messages will be sent to
     * @throws NotDirectoryException, IOException
     */
    public DirContentMonitor(Path directory, IndexEventsHandler indexEventsHandler, Logger logger)
            throws IOException {
        super(indexEventsHandler, logger);
        this.directory = directory;
        this.watchService = FileSystems.getDefault().newWatchService();
        if(directory == null) {
            throw new NotDirectoryException("null");
        }
        if(!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
            throw new NotDirectoryException(directory.toFile().getAbsolutePath());
        }
        registerDirectory(directory);
    }

    public DirContentMonitor(Path directory, IndexEventsHandler indexEventsHandler)
            throws IOException {
        this(directory, indexEventsHandler, null);
    }

    @Override
    public Path getTarget() {
        return directory;
    }

    /**
     * Starts directory monitoring loop. While events happen, they are handled by handler
     * passed in constructor. Monitoring process ends when all registered subdirectories'
     * keys become invalid or implicit interruption is performed (by calling stopMonitoring method)
     *
     * @throws NotHandledEventException if some events handling has been failed due to handler errors
     * or due to events overflow (too match of them is sent and WatchService just can not listen to all of them)
     */
    @Override
    public void startMonitoring() throws NotHandledEventException {
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

    /**
     * Stops monitoring process
     *
     * @throws IOException
     */
    @Override
    public void stopMonitoring() throws IOException {
        watchService.close();
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

    private void handleEvents(WatchKey key, Path registeredDir) throws NotHandledEventException {
        for (WatchEvent<?> event : key.pollEvents()) {
            WatchEvent.Kind kind = event.kind();
            if (kind == OVERFLOW) {
                throw new NotHandledEventException("events overflow");
            }
            WatchEvent<Path> pathEvent = castWatchEvent(event);
            Path relativeChildPath = pathEvent.context();
            Path childPath = registeredDir.resolve(relativeChildPath);
            if(kind == ENTRY_CREATE) {
                handleCreateWithRegistration(childPath);
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

    private void handleCreateWithRegistration(Path path) throws NotHandledEventException {
        try {
            if(Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                registerDirectory(path);
            }
        } catch (IOException e) {
            throw new NotHandledEventException(
                    "created directory registration failed due to IO error, details: " + e.getMessage());
        }
        handleCreateEvent(path);
    }
}
