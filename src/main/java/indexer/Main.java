package indexer;

import indexer.exceptions.InconsistentIndexException;
import indexer.exceptions.IndexClosedException;
import indexer.tokenizer.Word;
import indexer.tokenizer.WordsTokenizer;

import java.io.*;
import java.util.List;

/**
 * Created by mrx on 27.09.14.
 */
public class Main {
    public static void main(String[] args) {
        showHelp();
        try (FSIndexer fsIndexer = new FSIndexer(new WordsTokenizer(), System.out);
             BufferedReader br = new BufferedReader(new InputStreamReader(getStream(args)))) {
            while (true) {
                System.out.println("Enter command:");
                String input = br.readLine();
                String[] command = input.split(" ", 2);
                if (command[0].equals("h")) {
                    showHelp();
                } else if (command[0].equals("q")) {
                    return;
                } else if (command.length >= 2) {
                    handleCommandWithArg(fsIndexer, command);
                } else {
                    showUnknownCommandMsg();
                }
            }
        } catch (IOException e) {
            System.out.println("IO error: " + e.getMessage());
        } catch (IndexClosedException e) {
            System.out.println("Strange situation - index has been closed");
        } catch (InconsistentIndexException e) {
            System.out.println("Inconsistent index error, details: " + e.getMessage());
        }
    }

    private static InputStream getStream(String[] args) throws FileNotFoundException {
        if(args.length == 0) {
            return System.in;
        }
        return new FileInputStream(args[0]);
    }

    private static void showHelp() {
        final String help = "Commands:\n" +
                "a <file_or_dir_path> - add file or dir to index\n" +
                "r <file_or_dir_path> - remove file or dir from index\n" +
                "s <word>             - get list of files containing <word>\n" +
                "c <file_path>        - check if index contains file\n" +
                "h                    - show this help\n" +
                "q                    - finish work\n\n";
        System.out.println(help);
    }

    private static void handleCommandWithArg(FSIndexer fsIndexer, String[] command)
            throws IndexClosedException, InconsistentIndexException {
        switch (command[0]) {
            case "a":
                addCommand(fsIndexer, command[1]);
                break;
            case "r":
                removeCommand(fsIndexer, command[1]);
                break;
            case "s":
                searchCommand(fsIndexer, command[1]);
                break;
            case "c":
                containsCommand(fsIndexer, command[1]);
                break;
            default:
                showUnknownCommandMsg();
        }
    }

    private static void addCommand(FSIndexer fsIndexer, String file)
            throws IndexClosedException, InconsistentIndexException {
        System.out.println("Adding: " + file);
        try {
            fsIndexer.add(file);
            System.out.println("Added: " + file);
        } catch (IOException e) {
            System.out.println("File not added: " + file);
            System.out.println("Reason: " + e.getMessage());
        }
    }

    private static void removeCommand(FSIndexer fsIndexer, String file)
            throws IndexClosedException, InconsistentIndexException {
        System.out.println("Removing: " + file);
        try {
            fsIndexer.remove(file);
            System.out.println("Removed: " + file);
        } catch (IOException e) {
            System.out.println("File not removed: " + file);
            System.out.println("Reason: " + e.getMessage());
        }
    }

    private static void searchCommand(FSIndexer fsIndexer, String what)
            throws IndexClosedException, InconsistentIndexException {
        System.out.println("Searching: " + what);
        List<String> files = fsIndexer.search(new Word(what));
        if(files != null && files.size() != 0) {
            for (String f : files) {
                System.out.println(f);
            }
        } else {
            System.out.println("No files found");
        }
    }

    private static void containsCommand(FSIndexer fsIndexer, String file)
            throws IndexClosedException, InconsistentIndexException {
        System.out.println("Check contains: " + file);
        System.out.println(fsIndexer.containsFile(file));
    }

    private static void showUnknownCommandMsg() {
        System.out.println("Unknown command");
    }
}
