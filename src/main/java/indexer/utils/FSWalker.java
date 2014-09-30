package indexer.utils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Filesystem walker. Collects files list starting from specified root folder using multiple threads.
 * On walk started, collected files blocking queue may be obtained to listen the process of files adding.
 */
public class FSWalker {
    private final int WALKERS_NUM = Runtime.getRuntime().availableProcessors();
    private final AtomicInteger waitingWalkers = new AtomicInteger(WALKERS_NUM);
    private final ExecutorService walkersPool =
            Executors.newFixedThreadPool(WALKERS_NUM);
    private final Thread walkersManagerThread = new Thread(new WalkersManager());
    private final BlockingQueue<File> dirsQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<File> filesQueue = new LinkedBlockingQueue<>();

    private final AtomicBoolean someWalkerDoneHisWork = new AtomicBoolean(false);

    /**
     * Walks specified folder collecting files. Note that folderPath must not be null
     *
     * @param folderPath path of directory to walk
     */
    public void walk(Path folderPath) {
        if(!Files.isDirectory(folderPath)) {
            filesQueue.add(folderPath.toFile());
        } else {
            dirsQueue.add(folderPath.toFile());
            walkersManagerThread.start();
        }
    }

    public void stop() {
        walkersManagerThread.interrupt();
        walkersPool.shutdownNow();
    }

    public boolean isFinished() {
        return !walkersManagerThread.isAlive();
    }

    /**
     * Returns BlockingQueue containing files collected at the moment
     *
     * @return BlockingQueue containing files collected at the moment
     */
    public BlockingQueue<File> getCollectedFiles() {
        return filesQueue;
    }

    private class WalkersManager implements Runnable {
        @Override
        public void run() {
            try {
                while (true) {
                    while (!dirsQueue.isEmpty()) {
                        File dirToWalk = dirsQueue.take();
                        walkersPool.execute(new DirectoryWalker(Paths.get(dirToWalk.getAbsolutePath())));
                    }
                    synchronized (someWalkerDoneHisWork) {
                        while(!someWalkerDoneHisWork.get()) {
                            someWalkerDoneHisWork.wait();
                        }
                        someWalkerDoneHisWork.set(false);
                    }
                    if(dirsQueue.isEmpty() && waitingWalkers.get() == WALKERS_NUM) {
                        break;
                    }
                }
            } catch (InterruptedException e) {
            }
        }
    }

    private class DirectoryWalker implements Runnable {
        private final Path dirPath;

        /**
         * Path to directory must not be null
         *
         * @param dirPath path of directory to walk
         */
        public DirectoryWalker(Path dirPath) {
            this.dirPath = dirPath;
        }

        @Override
        public void run() {
            waitingWalkers.decrementAndGet();
            try {
                for (File entry : dirPath.toFile().listFiles()) {
                    if (entry.isDirectory()) {
                        dirsQueue.put(entry);
                    } else {
                        filesQueue.offer(entry);
                    }
                }
            } catch (InterruptedException e) {
            } finally {
                waitingWalkers.incrementAndGet();
                synchronized (someWalkerDoneHisWork) {
                    someWalkerDoneHisWork.set(true);
                    someWalkerDoneHisWork.notify();
                }
            }
        }
    }
}
