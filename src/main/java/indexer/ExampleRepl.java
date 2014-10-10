package indexer;

import indexer.exceptions.InconsistentIndexException;
import indexer.exceptions.IndexClosedException;
import indexer.tokenizer.Word;
import indexer.utils.ReadWriter;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by mrx on 30.09.14.
 */
public class ExampleRepl {
    private final List<Thread> execThreads = new LinkedList<>();
    private final Queue<List<String>> resultsQueue = new ConcurrentLinkedQueue<>();
    private final FSIndexer fsIndexer;

    private final AtomicInteger lastCommandId = new AtomicInteger(0);
    private int activeTasksCounter = 0;

    private boolean isInconsistentIndexException = false;
    private String inconsistentIndexMsg = "";
    private boolean isIndexClosedException = false;
    private String postponedExceptionMessage = null;
    
    private ReadWriter readWriter = null;

    public ExampleRepl(FSIndexer fsIndexer, ReadWriter readWriter) throws Exception {
        this.fsIndexer = fsIndexer;
        this.readWriter = readWriter;
    }

    public void start() {
        try {
            showHelp();
            while (true) {
                checkState();
                removeStoppedThreads();
                String[] command = readCommand();
                if (command == null || !handleCommand(command)) {
                    readWriter.println("Waiting active tasks to complete...");
                    return;
                }
            }
        } catch (Exception e) {
            postponedExceptionMessage = e.getMessage();
        } finally {
            releaseResources();
            if(postponedExceptionMessage != null) {
                System.out.println("Error: " + postponedExceptionMessage);
            } else {
                System.out.println("Done");
            }
        }
    }

    private boolean handleCommand(String[] command) throws IOException {
        if (command[0].equals("p")) {
            printCollectedResults();
        } else if(command[0].equals("h")) {
            showHelp();
        } else if(command[0].equals("q")) {
            return false;
        } else if(command.length >= 2) {
            handleTwoArgCommands(command);
        } else {
            printUnknownCommandMsg();
        }
        return true;
    }

    private void handleTwoArgCommands(String[] command) throws IOException {
        Thread execThread = null;
        if(command[0].equals("a")) {
            execThread = new Thread(new AddCommandRunner(lastCommandId.incrementAndGet(),
                    command[0], command[1].trim()));
        } else if(command[0].equals("r")) {
            execThread = new Thread(new RemoveCommandRunner(lastCommandId.incrementAndGet(),
                    command[0], command[1].trim()));
        } else if(command[0].equals("s")) {
            execThread = new Thread(new SearchCommandRunner(lastCommandId.incrementAndGet(),
                    command[0], command[1].trim()));
        } else if(command[0].equals("c")) {
            execThread = new Thread(new ContainsCommandRunner(lastCommandId.incrementAndGet(),
                    command[0], command[1].trim()));
        } else {
            printUnknownCommandMsg();
        }
        if(execThread != null) {
            execThread.start();
            execThreads.add(execThread);
            activeTasksCounter += 1;
            printCollectedResults();
        }
    }

    private void showHelp() throws IOException {
        final String help = "Commands:\n" +
                "a <file_or_dir_path> - add file or dir to index\n" +
                "r <file_or_dir_path> - remove file or dir from index\n" +
                "s <word>             - get list of files containing <word>\n" +
                "c <file_path>        - check if index contains file\n" +
                "p                    - show previous commands results\n" +
                "h                    - show this help\n" +
                "q                    - finish work";
        readWriter.println(help);
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

    private String[] readCommand() throws IOException {
        String input = readWriter.readLine();
        return input == null ? null : input.replace(" +", " ").trim().split(" ", 2);
    }

    private void printCollectedResults() throws IOException {
        readWriter.print("Previous commands results: ");
        if(resultsQueue.size() != 0) {
            readWriter.println("");
            while (!resultsQueue.isEmpty()) {
                List<String> entry = resultsQueue.poll();
                for(String s : entry) {
                    readWriter.println(s);
                }
                activeTasksCounter -= 1;
            }
        } else {
            readWriter.println("no results");
        }
        readWriter.println("Active tasks: " + activeTasksCounter);
    }

    private void printUnknownCommandMsg() throws IOException {
        readWriter.println("Unknown command");
    }

    private void releaseResources() {
        for(Thread et : execThreads) {
            try {
                et.join();
            } catch (InterruptedException e) {
            } catch (Exception e) {}
        }
    }

    private abstract class AsyncCommandRunner implements Runnable {
        protected final int cmdNumber;
        protected final String cmd;
        protected final String arg;

        public AsyncCommandRunner(int cmdNumber, String cmd, String arg) {
            this.cmdNumber = cmdNumber;
            this.cmd = cmd;
            this.arg = arg;
        }

        @Override
        public void run() {
            try {
                runCommand();
            } catch (InconsistentIndexException e) {
                isInconsistentIndexException = true;
                inconsistentIndexMsg = e.getMessage();
            } catch (IndexClosedException e) {
                isIndexClosedException = true;
            }
        }

        protected abstract void runCommand() throws IndexClosedException, InconsistentIndexException;
    }

    private class AddCommandRunner extends AsyncCommandRunner {
        public AddCommandRunner(int cmdNumber, String cmd, String arg) {
            super(cmdNumber, cmd, arg);
        }
        @Override
        protected void runCommand()
                throws IndexClosedException, InconsistentIndexException {
            try {
                fsIndexer.add(arg);
                resultsQueue.offer(Arrays.asList("#" + cmdNumber + "-Added: " + arg));
            } catch (IOException e) {
                resultsQueue.offer(Arrays.asList("#" + cmdNumber + "-File not added: " +
                                                 arg + "\nReason: " + e.getMessage()));
            }
        }
    }

    private class RemoveCommandRunner extends AsyncCommandRunner {
        public RemoveCommandRunner(int cmdNumber, String cmd, String arg) {
            super(cmdNumber, cmd, arg);
        }
        @Override
        protected void runCommand()
                throws IndexClosedException, InconsistentIndexException {
            try {
                fsIndexer.remove(arg);
                resultsQueue.offer(Arrays.asList("#" + cmdNumber + "-Removed: " + arg));
            } catch (IOException e) {
                resultsQueue.offer(Arrays.asList("#" + cmdNumber + "-File not removed: " +
                                                 arg + "\nReason: " + e.getMessage()));
            }
        }
    }

    private class SearchCommandRunner extends AsyncCommandRunner {
        public SearchCommandRunner(int cmdNumber, String cmd, String arg) {
            super(cmdNumber, cmd, arg);
        }
        @Override
        protected void runCommand()
                throws IndexClosedException, InconsistentIndexException {
            System.out.println("search started");
            List<String> files = fsIndexer.search(new Word(arg));
            System.out.println("result got");
            if(files != null && files.size() != 0) {
                String searchRes = "#" + cmdNumber + "-Files with token '" + arg + "':";
                files.add(0, searchRes);
                resultsQueue.offer(files);
            } else {
                resultsQueue.offer(Arrays.asList("#" + cmdNumber + "-No files found"));
            }
        }
    }

    private class ContainsCommandRunner extends AsyncCommandRunner {
        public ContainsCommandRunner(int cmdNumber, String cmd, String arg) {
            super(cmdNumber, cmd, arg);
        }
        @Override
        protected void runCommand()
                throws IndexClosedException, InconsistentIndexException {
            resultsQueue.offer(Arrays.asList("#" + cmdNumber + "-Contains " + arg + ": " + fsIndexer.containsFile(arg)));
        }
    }
}