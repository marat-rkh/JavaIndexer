package indexer.fsmonitor;

import indexer.exceptions.NotHandledEventException;
import indexer.handler.IndexEventsHandler;
import indexer.utils.Logger;

import java.io.IOException;
import java.nio.file.Path;

/**
 * FSMonitor implementation for listening single folder with it's whole content or just a single file.
 * Consists of two FSMonitor's: RootMonitor and DirContentMonitor. The second one
 * can only listen directory content events. So the first one listens for remove
 * event of folder itself or just a single file.
 *
 * @see indexer.fsmonitor.RootMonitor
 * @see indexer.fsmonitor.DirContentMonitor
 */
public class DirMonitor implements FSMonitor {
    private final FSMonitor rootMonitor;
    private final FSMonitor contentsMonitor;
    private final Path target;

    private final Object syncObject = new Object();
    private boolean isStopped = false;
    private final StringBuffer lastExceptionMsgBuffer = new StringBuffer();

    public DirMonitor(Path targetPath, IndexEventsHandler indexEventsHandler, Logger logger,
                      FSMonitorLifecycleHandler monitorHandler) throws IOException {
        this.rootMonitor = new RootMonitor(targetPath, indexEventsHandler, logger, monitorHandler);
        if(targetPath.toFile().isDirectory()) {
            this.contentsMonitor = new DirContentMonitor(targetPath, indexEventsHandler, logger);
        } else {
            this.contentsMonitor = null;
        }
        this.target = targetPath;
    }
    @Override
    public Path getTarget() {
        return target;
    }

    @Override
    public void startMonitoring() throws NotHandledEventException {
        reset();
        startMonitorThread(rootMonitor);
        if(contentsMonitor != null) {
            startMonitorThread(contentsMonitor);
        }
        synchronized (syncObject) {
            while (!isNotHandledEventInMonitors() && !isStopped) {
                try {
                    syncObject.wait();
                } catch (InterruptedException e) {
                    reset();
                    return;
                }
            }
        }
        if(isNotHandledEventInMonitors()) {
            throw new NotHandledEventException(lastExceptionMsgBuffer.toString());
        }
    }

    @Override
    public void stopMonitoring() throws IOException {
        isStopped = true;
        rootMonitor.stopMonitoring();
        if(contentsMonitor != null) {
            contentsMonitor.stopMonitoring();
        }
        synchronized (syncObject) {
            syncObject.notify();
        }
    }

    private void reset() {
        isStopped = false;
        lastExceptionMsgBuffer.setLength(0);
    }

    private void startMonitorThread(final FSMonitor monitor) {
        Thread newThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    monitor.startMonitoring();
                } catch (NotHandledEventException e) {
                    synchronized (syncObject) {
                        lastExceptionMsgBuffer.append(e.getMessage());
                        syncObject.notify();
                    }
                }
            }
        });
        newThread.start();
    }

    private boolean isNotHandledEventInMonitors() { return lastExceptionMsgBuffer.length() != 0; }
}
