package indexer;

import indexer.exceptions.InconsistentIndexException;
import indexer.exceptions.IndexClosedException;
import indexer.tokenizer.Word;
import indexer.utils.ReadWriter;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
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

    private AtomicBoolean userWaitsResult = new AtomicBoolean(false);
    private UserWaitingCommandRunner lastCommandRunner = null;

    private boolean isInconsistentIndexException = false;
    private String inconsistentIndexMsg = "";
    private boolean isIndexClosedException = false;
    private boolean isIoException = false;
    private String ioExceptionMsg = null;
    private String postponedExceptionMessage = null;

    private ReadWriter readWriter = null;

    public ExampleRepl(FSIndexer fsIndexer, final ReadWriter readWriter) throws Exception {
        this.fsIndexer = fsIndexer;
        this.readWriter = readWriter;
        this.readWriter.addCharListener(";", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                userWaitsResult.set(false);
                if(lastCommandRunner != null) {
                    lastCommandRunner.setUserDoesntWait();
                }
            }
        });
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
        } else if(command[0].equals("t")) {
            showActiveTasks();
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
            lastCommandRunner = new AddCommandRunner(lastCommandId.incrementAndGet(), command[0], command[1].trim());
            execThread = new Thread(lastCommandRunner);
        } else if(command[0].equals("r")) {
            lastCommandRunner = new RemoveCommandRunner(lastCommandId.incrementAndGet(), command[0], command[1].trim());
            execThread = new Thread(lastCommandRunner);
        } else if(command[0].equals("s")) {
            lastCommandRunner = new SearchCommandRunner(lastCommandId.incrementAndGet(), command[0], command[1].trim());
            execThread = new Thread(lastCommandRunner);
        } else if(command[0].equals("c")) {
            lastCommandRunner = new ContainsCommandRunner(lastCommandId.incrementAndGet(), command[0], command[1].trim());
            execThread = new Thread(lastCommandRunner);
        } else {
            printUnknownCommandMsg();
        }
        if(execThread != null) {
            execute(command, execThread);
        }
    }

    private void showHelp() throws IOException {
        final String help = "Commands:\n" +
                "a <file_or_dir_path> - add file or dir to index\n" +
                "r <file_or_dir_path> - remove file or dir from index\n" +
                "s <word>             - get list of files containing <word>\n" +
                "c <file_path>        - check if index contains file\n" +
                "p                    - show previous commands results\n" +
                "t                    - show active tasks\n" +
                "h                    - show this help\n" +
                "q                    - finish work";
        readWriter.println(help);
    }

    private void checkState() throws Exception {
        if(isInconsistentIndexException) {
            throw new Exception("Inconsistent index error, details: " + inconsistentIndexMsg);
        } else if(isIndexClosedException) {
            throw new Exception("Strange situation - index has been closed");
        } else if(isIoException) {
            throw new Exception("Error occurred while printing command results, details: " + ioExceptionMsg);
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
        String input = readWriter.interact();
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

    private void showActiveTasks() throws IOException {
        readWriter.println("Active tasks:");
        int activeTasksNum = 0;
        for(Thread et : execThreads) {
            if(et.isAlive()) {
                activeTasksNum += 1;
                readWriter.println(et.getName());
            }
        }
        readWriter.println("Total: " + activeTasksNum);
    }

    private void execute(String[] command, Thread execThread) throws IOException {
        execThread.setName("#" + lastCommandId.get() + "(" + command[0] + " " + command[1] + ")");
        execThreads.add(execThread);
        activeTasksCounter += 1;

        userWaitsResult.set(true);
        execThread.start();
        while (userWaitsResult.get() && !lastCommandRunner.resultsPrintedForUser()) {
            readWriter.println("...Waiting results (to watch them later press ';' and Enter)");
            readWriter.readLine();
        }
        userWaitsResult.set(false);
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
        protected final int CMD_NUMBER;
        protected final String CMD;
        protected final String ARG;
        protected final String CMD_DESCRIPTION;

        public AsyncCommandRunner(int cmdNumber, String cmd, String arg) {
            this.CMD_NUMBER = cmdNumber;
            this.CMD = cmd;
            this.ARG = arg;
            this.CMD_DESCRIPTION = "\n#" + CMD_NUMBER + "(" + CMD + " " + ARG + ")";
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

    private abstract class UserWaitingCommandRunner extends AsyncCommandRunner {
        protected boolean userWaitsForResult = true;
        protected boolean printingResultsFinished = false;

        public UserWaitingCommandRunner(int cmdNumber, String cmd, String arg) {
            super(cmdNumber, cmd, arg);
        }

        protected void printOrEnqueue(List<String> results) {
            if(userWaitsForResult) {
                try {
                    for (String r : results) {
                        readWriter.println(r);
                    }
                    readWriter.print("\n...All results are received, press Enter to continue");
                } catch (IOException e) {
                    isIoException = true;
                    ioExceptionMsg = e.getMessage();
                } finally {
                    printingResultsFinished = true;
                    activeTasksCounter -= 1;
                }
            } else {
                resultsQueue.offer(results);
            }
        }

        public void setUserDoesntWait() { userWaitsForResult = false; }
        public boolean resultsPrintedForUser() { return printingResultsFinished; }
    }

    private class AddCommandRunner extends UserWaitingCommandRunner {
        public AddCommandRunner(int cmdNumber, String cmd, String arg) {
            super(cmdNumber, cmd, arg);
        }
        @Override
        protected void runCommand()
                throws IndexClosedException, InconsistentIndexException {
            try {
                fsIndexer.add(ARG);
                printOrEnqueue(Arrays.asList(CMD_DESCRIPTION + "-Added"));
            } catch (IOException e) {
                printOrEnqueue(Arrays.asList(CMD_DESCRIPTION + "-File not added \nReason: " + e.getMessage()));
            }
        }
    }

    private class RemoveCommandRunner extends UserWaitingCommandRunner {
        public RemoveCommandRunner(int cmdNumber, String cmd, String arg) {
            super(cmdNumber, cmd, arg);
        }
        @Override
        protected void runCommand()
                throws IndexClosedException, InconsistentIndexException {
            try {
                fsIndexer.remove(ARG);
                printOrEnqueue(Arrays.asList(CMD_DESCRIPTION + "-Removed"));
            } catch (IOException e) {
                printOrEnqueue(Arrays.asList(CMD_DESCRIPTION + "-File not removed \nReason: " + e.getMessage()));
            }
        }
    }

    private class SearchCommandRunner extends UserWaitingCommandRunner {
        public SearchCommandRunner(int cmdNumber, String cmd, String arg) {
            super(cmdNumber, cmd, arg);
        }
        @Override
        protected void runCommand()
                throws IndexClosedException, InconsistentIndexException {
            List<String> files = fsIndexer.search(new Word(ARG));
            if(files != null && files.size() != 0) {
                String searchRes = CMD_DESCRIPTION + "-Files with token '" + ARG + "':";
                files.add(0, searchRes);
                printOrEnqueue(files);
            } else {
                printOrEnqueue(Arrays.asList(CMD_DESCRIPTION + "-No files found"));
            }
        }
    }

    private class ContainsCommandRunner extends UserWaitingCommandRunner {
        public ContainsCommandRunner(int cmdNumber, String cmd, String arg) {
            super(cmdNumber, cmd, arg);
        }
        @Override
        protected void runCommand()
                throws IndexClosedException, InconsistentIndexException {
            printOrEnqueue(Arrays.asList(CMD_DESCRIPTION + "-Contains: " + fsIndexer.containsFile(ARG)));
        }
    }
}