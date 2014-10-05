package indexer;

import indexer.exceptions.InconsistentIndexException;
import indexer.exceptions.IndexClosedException;
import indexer.tokenizer.Word;
import indexer.utils.ReadWriter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by mrx on 30.09.14.
 */
public class ExampleRepl {
    private final List<Thread> execThreads = new LinkedList<>();
    private final Queue<String> resultsQueue = new ConcurrentLinkedQueue<>();
    private final ExtendedFSIndexer fsIndexer;

    private final AtomicInteger lastCommandId = new AtomicInteger(0);
    private int activeTasksCounter = 0;

    private boolean isInconsistentIndexException = false;
    private String inconsistentIndexMsg = "";
    private boolean isIndexClosedException = false;
    private String postponedExceptionMessage = null;
    
    private ReadWriter readWriter = null;

    public ExampleRepl(ExtendedFSIndexer fsIndexer, ReadWriter readWriter) throws Exception {
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
        } else if(command[0].equals("m+")) {
            fsIndexer.useMimeTypes();
            readWriter.println("Mime types mode: on");
        } else if(command[0].equals("m-")) {
            fsIndexer.useExtensions();
            readWriter.println("Mime types mode: off (using extensions)");
        } else if(command[0].equals("e")) {
            Set<String> exts = fsIndexer.getCurrentExtensions();
            if(exts.size() != 0) {
                for (String e : exts) {
                    readWriter.print(e + " ");
                }
                readWriter.println("");
            } else {
                readWriter.println("Extensions list is empty");
            }
        } else if(command.length >= 2) {
            Thread execThread = null;
            if(command[0].equals("e+")) {
                fsIndexer.addExtensions(splitIntoList(command[1]));
                readWriter.println("Extensions added");
            } else if (command[0].equals("e-")) {
                fsIndexer.removeExtensions(splitIntoList(command[1]));
                readWriter.println("Extensions removed");
            } else if(command[0].equals("a")) {
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
        } else {
            printUnknownCommandMsg();
        }
        return true;
    }

    private void showHelp() throws IOException {
        final String help = "Commands:\n" +
                "a <file_or_dir_path> - add file or dir to index\n\n" +
                "r <file_or_dir_path> - remove file or dir from index\n\n" +
                "s <word>             - get list of files containing <word>\n\n" +
                "c <file_path>        - check if index contains file\n\n" +
                "p                    - show previous commands results\n\n" +
                "m+                   - turn on mime types mode (turned on by default)\n" +
                "In this mode when passing a folder path to command 'a' only files with text mime type are added\n\n" +
                "m-                   - turn off mime types mode\n" +
                "Extensions added with 'e+' command will be used when adding folders\n\n" +
                "e+ <ext1> <ext2> ... - add extensions\n" +
                "If mime types mode is off only files with listed extensions will be added when passing a folder path\n" +
                "to command 'a' (if adding a single file listed extensions are ignored)\n" +
                "Note that multiple calls don't cancel previous settings\n" +
                "Example: e+ java txt xml\n\n" +
                "e- <ext1> <ext2> ... - remove extensions added with command 'e+'\n\n" +
                "e                    - list extensions used for directories adding\n\n" +
                "h                    - show this help\n\n" +
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
                readWriter.println(resultsQueue.poll());
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

    private List<String> splitIntoList(String str) {
        String[] arr = str.split(" ");
        List<String> list = new ArrayList<>();
        for(String a : arr) {
            list.add(a.trim());
        }
        return list;
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
                resultsQueue.offer("#" + cmdNumber + "-Added: " + arg);
            } catch (IOException e) {
                resultsQueue.offer("#" + cmdNumber + "-File not added: " + arg + "\nReason: " + e.getMessage());
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
                resultsQueue.offer("#" + cmdNumber + "-Removed: " + arg);
            } catch (IOException e) {
                resultsQueue.offer("#" + cmdNumber + "-File not removed: " + arg + "\nReason: " + e.getMessage());
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
    }

    private class ContainsCommandRunner extends AsyncCommandRunner {
        public ContainsCommandRunner(int cmdNumber, String cmd, String arg) {
            super(cmdNumber, cmd, arg);
        }
        @Override
        protected void runCommand()
                throws IndexClosedException, InconsistentIndexException {
            resultsQueue.offer("#" + cmdNumber + "-Contains " + arg + ": " + fsIndexer.containsFile(arg));
        }
    }
}