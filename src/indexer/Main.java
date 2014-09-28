package indexer;

import indexer.exceptions.InconsistentIndexException;
import indexer.exceptions.IndexClosedException;
import indexer.tokenizer.Word;
import indexer.tokenizer.WordsTokenizer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;

/**
 * Created by mrx on 27.09.14.
 */
public class Main {
    public static void main(String[] args) {
        showHelp();
        try (FSIndexer fsIndexer = new FSIndexer(new WordsTokenizer(), System.out);
             BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
                System.out.println("Enter command:");
                String input = br.readLine();
                String[] command = input.split(" ");
                if (command[0].equals("h")) {
                    showHelp();
                } else if (command[0].equals("q")) {
                    return;
                } else if (command.length >= 2) {
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
                } else {
                    showUnknownCommandMsg();
                }
            }
        } catch (IOException e) {
            System.out.println("IO error occurred. Details: " + e.getMessage());
        } catch (IndexClosedException e) {
            System.out.println("Strange situation - index has been closed");
        } catch (InconsistentIndexException e) {
            System.out.println("Inconsistent index error, details: " + e.getMessage());
        }
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

    private static void addCommand(FSIndexer fsIndexer, String file)
            throws IndexClosedException, IOException, InconsistentIndexException {
        fsIndexer.add(file);
        System.out.println("Added: " + file);
    }

    private static void removeCommand(FSIndexer fsIndexer, String file)
            throws IndexClosedException, IOException, InconsistentIndexException {
        fsIndexer.remove(file);
        System.out.println("Removed: " + file);
    }

    private static void searchCommand(FSIndexer fsIndexer, String what)
            throws IndexClosedException, InconsistentIndexException {
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
        System.out.println(fsIndexer.containsFile(file));
    }

    private static void showUnknownCommandMsg() {
        System.out.println("Unknown command");
    }
}
