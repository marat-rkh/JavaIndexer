package indexer;

import indexer.exceptions.InconsistentIndexException;
import indexer.exceptions.IndexClosedException;
import indexer.tokenizer.Word;

import java.io.*;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by mrx on 30.09.14.
 */
public class ExampleRepl {
    private final List<Thread> execThreads = new LinkedList<>();
    private final Queue<String> resultsQueue = new ConcurrentLinkedQueue<>();
    private final FSIndexer fsIndexer;
    private final AtomicInteger lastCommandId = new AtomicInteger(0);

    private boolean isInconsistentIndexException = false;
    private String inconsistentIndexMsg = "";
    private boolean isIndexClosedException = false;

    public ExampleRepl(FSIndexer fsIndexer) {
        this.fsIndexer = fsIndexer;
    }

    public void start(String commandsFilePath) {
        showHelp();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(getStream(commandsFilePath)))) {
            while (true) {
                checkState();
                removeStoppedThreads();
                String[] command = readCommand(br);
                if (command[0].equals("p")) {
                    printCollectedResults();
                } else if (command[0].equals("h")) {
                    showHelp();
                } else if (command[0].equals("q")) {
                    return;
                } else if (command.length >= 2) {
                    IndexCommandsRunner runner = new IndexCommandsRunner(lastCommandId.incrementAndGet(),
                                                                         command[0], command[1]);
                    Thread execThread = new Thread(runner);
                    execThread.start();
                    execThreads.add(execThread);
                    System.out.println("Command #" + lastCommandId.get() + " is queued");
                    printCollectedResults();
                } else {
                    showUnknownCommandMsg();
                }
            }
        } catch (IOException e) {
            System.out.println("IO error: " + e.getMessage());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } finally {
            System.out.println("Waiting active tasks to complete...");
            for(Thread et : execThreads) {
                try {
                    et.join();
                } catch (InterruptedException e) {
                }
            }
            System.out.println("Done");
        }
    }

    private void showHelp() {
        final String help = "Commands:\n" +
                "a <file_or_dir_path> - add file or dir to index\n" +
                "r <file_or_dir_path> - remove file or dir from index\n" +
                "s <word>             - get list of files containing <word>\n" +
                "c <file_path>        - check if index contains file\n" +
                "p                    - show previous commands results\n" +
                "h                    - show this help\n" +
                "q                    - finish work";
        System.out.println(help);
    }

    private InputStream getStream(String commandsFilePath) throws FileNotFoundException {
        if(commandsFilePath == null) {
            return System.in;
        }
        return new FileInputStream(commandsFilePath);
    }

    private void checkState() throws Exception {
        if(isInconsistentIndexException) {
            throw new Exception("Inconsistent index error, details: " + inconsistentIndexMsg);
        } else if(isIndexClosedException) {
            throw new Exception("Strange situation - index has been closed");
        }
    }

    private void removeStoppedThreads() {
        Iterator<Thread> it = execThreads.iterator();
        while (it.hasNext()) {
            Thread et = it.next();
            if(!et.isAlive()) {
                it.remove();
            }
        }
    }

    private String[] readCommand(BufferedReader br) throws IOException {
        System.out.println("\n$Enter command:");
        String input = br.readLine();
        return input.split(" ", 2);
    }

    private void printCollectedResults() {
        System.out.println("Previous commands results:");
        if(resultsQueue.size() != 0) {
            while (!resultsQueue.isEmpty()) {
                System.out.println(resultsQueue.poll());
            }
        } else {
            System.out.println("no results");
        }
    }

    private void showUnknownCommandMsg() {
        resultsQueue.offer("Unknown command");
    }

    private class IndexCommandsRunner implements Runnable {
        private final int cmdNumber;
        private final String cmd;
        private final String arg;

        public IndexCommandsRunner(int cmdNumber, String cmd, String arg) {
            this.cmdNumber = cmdNumber;
            this.cmd = cmd;
            this.arg = arg;
        }

        @Override
        public void run() {
            try {
                switch (cmd) {
                    case "a":
                        addCommand();
                        break;
                    case "r":
                        removeCommand();
                        break;
                    case "s":
                        searchCommand();
                        break;
                    case "c":
                        containsCommand();
                        break;
                    default:
                        showUnknownCommandMsg();
                }
            } catch (InconsistentIndexException e) {
                isInconsistentIndexException = true;
                inconsistentIndexMsg = e.getMessage();
            } catch (IndexClosedException e) {
                isIndexClosedException = true;
            }
        }

        private void addCommand()
                throws IndexClosedException, InconsistentIndexException {
            try {
                fsIndexer.add(arg);
                resultsQueue.offer("#" + cmdNumber + "-Added: " + arg);
            } catch (IOException e) {
                resultsQueue.offer("#" + cmdNumber + "-File not added: " + arg + "\nReason: " + e.getMessage());
            }
        }

        private void removeCommand()
                throws IndexClosedException, InconsistentIndexException {
            try {
                fsIndexer.remove(arg);
                resultsQueue.offer("#" + cmdNumber + "-Removed: " + arg);
            } catch (IOException e) {
                resultsQueue.offer("#" + cmdNumber + "-File not removed: " + arg + "\nReason: " + e.getMessage());
            }
        }

        private void searchCommand()
                throws IndexClosedException, InconsistentIndexException {
            List<String> files = fsIndexer.search(new Word(arg));
            if(files != null && files.size() != 0) {
                String searchRes = "#" + cmdNumber + "-Files with token '" + arg + "':\n";
                for (String f : files) {
                    searchRes += (f + "\n");
                }
                resultsQueue.offer(searchRes);
            } else {
                resultsQueue.offer("#" + cmdNumber + "-No files found");
            }
        }

        private void containsCommand()
                throws IndexClosedException, InconsistentIndexException {
            resultsQueue.offer("#" + cmdNumber + "-Contains " + arg + ": " + fsIndexer.containsFile(arg));
        }
    }
}
