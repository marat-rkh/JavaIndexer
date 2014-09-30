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
 * On startWalking called, collected files are put in BlockingQueue passed to the class instance constructor.
 * Note when filesystem traversal is finished FSWalker will put special fake file in output BlockingQueue to
 * indicate traversal end event. This fake file can be obtained by calling FSWalker.FAKE_END_FILE. So class
 * users can use FSWalker.FAKE_END_FILE to check if current collected file equals to it (and if so understand
 * that all files have been already collected)
 */
public class FSWalker {
    private final int WALKERS_NUM = Runtime.getRuntime().availableProcessors();
    private final AtomicInteger waitingWalkers = new AtomicInteger(WALKERS_NUM);
    private final ExecutorService walkersPool =
            Executors.newFixedThreadPool(WALKERS_NUM);
    private final Thread walkersManagerThread = new Thread(new WalkersManager());
    private final BlockingQueue<File> dirsQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<File> filesQueue;
    public static final File FAKE_END_FILE = new File("");

    private final AtomicBoolean someWalkerDoneHisWork = new AtomicBoolean(false);

    public FSWalker(BlockingQueue<File> filesQueue) {
        this.filesQueue = filesQueue;
    }

    /**
     * Starts walking specified folder collecting files. Note that folderPath must not be null
     *
     * @param folderPath path of directory to start walking
     */
    public void startWalking(Path folderPath) {
        if(!Files.isDirectory(folderPath)) {
            filesQueue.add(folderPath.toFile());
            filesQueue.add(FAKE_END_FILE);
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
            } finally {
                filesQueue.add(FAKE_END_FILE);
            }
        }
    }

    private class DirectoryWalker implements Runnable {
        private final Path dirPath;

        /**
         * Path to directory must not be null
         *
         * @param dirPath path of directory to startWalking
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
                        filesQueue.put(entry);
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
