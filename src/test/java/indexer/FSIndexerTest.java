package indexer;

import indexer.tokenizer.Token;
import indexer.tokenizer.Word;
import indexer.tokenizer.WordsTokenizer;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class FSIndexerTest extends TmpFsCreator {
    @Test
    public void testSearch() throws Exception {
        final FSIndexer fsIndexer = new FSIndexer(new WordsTokenizer(), null);
        fsIndexer.add(tempFolder.getRoot().getAbsolutePath());
        final AtomicInteger counter = new AtomicInteger(0);
        Runnable searchQuery = createSearchRunnable(fsIndexer, counter, new Word("file1"));
        runTestThreads(Arrays.asList(searchQuery, searchQuery, searchQuery));
        assertEquals(3, counter.get());
        fsIndexer.close();
    }

    @Test
    public void testReadQueries() throws Exception {
        final FSIndexer fsIndexer = new FSIndexer(new WordsTokenizer(), null);
        fsIndexer.add(tempFolder.getRoot().getAbsolutePath());
        final AtomicInteger searchCounter = new AtomicInteger(0);
        Runnable searchQuery1 = createSearchRunnable(fsIndexer, searchCounter, new Word("file1"));
        Runnable searchQuery2 = createSearchRunnable(fsIndexer, searchCounter, new Word("file2"));
        final AtomicInteger containsCounter = new AtomicInteger(0);
        Runnable containsQuery1 = createContainsRunnable(fsIndexer, containsCounter, file1.getAbsolutePath());
        Runnable containsQuery2 = createContainsRunnable(fsIndexer, containsCounter, file2.getAbsolutePath());
        Runnable containsQuery3 = createContainsRunnable(fsIndexer, containsCounter, dir1SubFile1.getAbsolutePath());
        runTestThreads(Arrays.asList(containsQuery1, searchQuery1, containsQuery2, searchQuery2, containsQuery3));
        assertEquals(2, searchCounter.get());
        assertEquals(3, containsCounter.get());
        fsIndexer.close();
    }

    @Test
    public void testAdd() throws Exception {
        final FSIndexer fsIndexer = new FSIndexer(new WordsTokenizer(), null);
        final AtomicInteger addCounter = new AtomicInteger(0);
        Runnable add1 = createAddRunnable(fsIndexer, addCounter, dir1.getAbsolutePath());
        Runnable add2 = createAddRunnable(fsIndexer, addCounter, dir2SubFile1.getAbsolutePath());
        Runnable add3 = createAddRunnable(fsIndexer, addCounter, file2.getAbsolutePath());
        runTestThreads(Arrays.asList(add1, add2, add3));
        assertEquals(3, addCounter.get());
        fsIndexer.close();
    }

    @Test
    public void testRemove() throws Exception {
        final FSIndexer fsIndexer = new FSIndexer(new WordsTokenizer(), null);
        fsIndexer.add(tempFolder.getRoot().getAbsolutePath());
        final AtomicInteger removeCounter = new AtomicInteger(0);
        Runnable rm1 = createRemoveRunnable(fsIndexer, removeCounter, dir1.getAbsolutePath());
        Runnable rm2 = createRemoveRunnable(fsIndexer, removeCounter, dir2SubFile1.getAbsolutePath());
        Runnable rm3 = createRemoveRunnable(fsIndexer, removeCounter, file2.getAbsolutePath());
        runTestThreads(Arrays.asList(rm1, rm2, rm3));
        assertEquals(3, removeCounter.get());
        fsIndexer.close();
    }

    @Test
    public void testReadWrite() throws Exception {
        final FSIndexer fsIndexer = new FSIndexer(new WordsTokenizer(), null);
        fsIndexer.add(dir1.getAbsolutePath());
        fsIndexer.add(dir2.getAbsolutePath());
        final AtomicInteger searchCounter = new AtomicInteger(0);
        Runnable searchQuery1 = createSearchRunnable(fsIndexer, searchCounter, new Word("Lorem"));
        Runnable searchQuery2 = createSearchRunnable(fsIndexer, searchCounter, new Word("Lorem"));
        Runnable searchQuery3 = createSearchRunnable(fsIndexer, searchCounter, new Word("ipsum"));
        final AtomicInteger addCounter = new AtomicInteger(0);
        Runnable addQuery1 = createAddRunnable(fsIndexer, addCounter, file1.getAbsolutePath());
        Runnable addQuery2 = createAddRunnable(fsIndexer, addCounter, tempFolder.getRoot().getAbsolutePath());
        Runnable addQuery3 = createAddRunnable(fsIndexer, addCounter, tempFolder.getRoot().getAbsolutePath());
        runTestThreads(Arrays.asList(searchQuery1, addQuery1, searchQuery2, addQuery2, addQuery3, searchQuery3));
        assertEquals(3, searchCounter.get());
        assertEquals(3, addCounter.get());
        assertEquals(3, fsIndexer.search(new Word("content")).size());
        fsIndexer.close();
    }

    @Test
    public void testFastQueries() throws Exception {
        final FSIndexer fsIndexer = new FSIndexer(new WordsTokenizer(), null);
        fsIndexer.add(dir2.getAbsolutePath());
        final AtomicInteger searchCounter = new AtomicInteger(0);
        final int repeats = 10000;
        Runnable fastSearchQuery = fastSearchRunnable(fsIndexer, searchCounter, new Word("amet,"), repeats);
        final AtomicInteger addCounter = new AtomicInteger(0);
        Runnable addRemoveQuery = delayedAddRemoveRunnable(fsIndexer, addCounter, dir1.getAbsolutePath(), dir1.getAbsolutePath());
        runTestThreads(Arrays.asList(fastSearchQuery, addRemoveQuery));
        assertEquals(10000, searchCounter.get());
        assertEquals(2, addCounter.get());
        assertFalse(fsIndexer.containsFile(dir1SubFile1.getAbsolutePath()));
        fsIndexer.close();
    }

    @Test
    public void testAllQueries() throws Exception {
        final FSIndexer fsIndexer = new FSIndexer(new WordsTokenizer(), null);
        fsIndexer.add(dir2.getAbsolutePath());
        assertTrue(fsIndexer.containsFile(dir2SubFile1.getAbsolutePath()));
        assertEquals(1, fsIndexer.search(new Word("dolor")).size());
        fsIndexer.add(dir1SubFile1.getAbsolutePath());
        assertTrue(fsIndexer.containsFile(dir1SubFile1.getAbsolutePath()));
        assertTrue(fsIndexer.containsFile(dir2SubFile1.getAbsolutePath()));
        assertEquals(1, fsIndexer.search(new Word("dolor")).size());
        fsIndexer.add(tempFolder.getRoot().getAbsolutePath());
        assertTrue(fsIndexer.containsFile(file1.getAbsolutePath()));
        assertTrue(fsIndexer.containsFile(file2.getAbsolutePath()));
        assertTrue(fsIndexer.containsFile(file3.getAbsolutePath()));
        assertTrue(fsIndexer.containsFile(dir1SubFile1.getAbsolutePath()));
        assertTrue(fsIndexer.containsFile(dir2SubFile1.getAbsolutePath()));
        fsIndexer.remove(dir1.getAbsolutePath());
        assertFalse(fsIndexer.containsFile(dir1SubFile1.getAbsolutePath()));
        fsIndexer.remove(dir2SubFile1.getAbsolutePath());
        assertFalse(fsIndexer.containsFile(dir2SubFile1.getAbsolutePath()));
        assertEquals(0, fsIndexer.search(new Word("dolor")).size());
        fsIndexer.remove(tempFolder.getRoot().getAbsolutePath());

        assertEquals(0, fsIndexer.search(new Word("dolor")).size());
        assertEquals(0, fsIndexer.search(new Word("file1")).size());
        assertEquals(0, fsIndexer.search(new Word("file2")).size());
        assertEquals(0, fsIndexer.search(new Word("file3")).size());
        assertFalse(fsIndexer.containsFile(file1.getAbsolutePath()));
        assertFalse(fsIndexer.containsFile(file2.getAbsolutePath()));
        assertFalse(fsIndexer.containsFile(file3.getAbsolutePath()));
        assertFalse(fsIndexer.containsFile(dir1SubFile1.getAbsolutePath()));
        assertFalse(fsIndexer.containsFile(dir2SubFile1.getAbsolutePath()));
    }

    private Runnable createSearchRunnable(final FSIndexer fsIndexer, final AtomicInteger counterToModify,
                                          final Token token) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    int results = fsIndexer.search(token).size();
                    int old = counterToModify.get();
                    while (!counterToModify.compareAndSet(old, old + results)) {
                        old = counterToModify.get();
                    }
                } catch (Exception e) {
                    int old = counterToModify.get();
                    while (!counterToModify.compareAndSet(old, -1000)) {
                        old = counterToModify.get();
                    }
                }
            }
        };
    }

    private Runnable createContainsRunnable(final FSIndexer fsIndexer, final AtomicInteger counterToModify,
                                            final String file) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    if(fsIndexer.containsFile(file)) {
                        counterToModify.incrementAndGet();
                    }
                } catch (Exception e) {
                    int old = counterToModify.get();
                    while (!counterToModify.compareAndSet(old, -1000)) {
                        old = counterToModify.get();
                    }
                }
            }
        };
    }

    private Runnable createAddRunnable(final FSIndexer fsIndexer, final AtomicInteger counterToModify,
                                       final String file) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    fsIndexer.add(file);
                    counterToModify.incrementAndGet();
                } catch (Exception e) {
                    int old = counterToModify.get();
                    while (!counterToModify.compareAndSet(old, -1000)) {
                        old = counterToModify.get();
                    }
                }
            }
        };
    }

    private Runnable createRemoveRunnable(final FSIndexer fsIndexer, final AtomicInteger counterToModify,
                                          final String file) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    fsIndexer.remove(file);
                    counterToModify.incrementAndGet();
                } catch (Exception e) {
                    int old = counterToModify.get();
                    while (!counterToModify.compareAndSet(old, -1000)) {
                        old = counterToModify.get();
                    }
                }
            }
        };
    }

    private Runnable fastSearchRunnable(final FSIndexer fsIndexer, final AtomicInteger counterToModify,
                                        final Token token, final int times) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    for(int i = 0; i < times; i++) {
                        int results = fsIndexer.search(token).size();
                        int old = counterToModify.get();
                        while (!counterToModify.compareAndSet(old, old + results)) {
                            old = counterToModify.get();
                        }
                    }
                } catch (Exception e) {
                    int old = counterToModify.get();
                    while (!counterToModify.compareAndSet(old, -1000)) {
                        old = counterToModify.get();
                    }
                }
            }
        };
    }

    private Runnable delayedAddRemoveRunnable(final FSIndexer fsIndexer, final AtomicInteger counterToModify,
                                              final String fileToAdd, final String fileToRemove) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(50);
                    fsIndexer.add(fileToAdd);
                    counterToModify.incrementAndGet();
                    Thread.sleep(50);
                    fsIndexer.remove(fileToRemove);
                    counterToModify.incrementAndGet();
                } catch (Exception e) {
                    int old = counterToModify.get();
                    while (!counterToModify.compareAndSet(old, -1000)) {
                        old = counterToModify.get();
                    }
                }
            }
        };
    }

    private void runTestThreads(List<Runnable> runnables) throws InterruptedException {
        List<Thread> threads = new ArrayList<Thread>();
        for(int i = 0; i < runnables.size(); i++) {
            threads.add(new Thread(runnables.get(i)));
        }
        for(Thread t : threads) {
            t.start();
        }
        for(Thread t : threads) {
            t.join();
        }
    }
}