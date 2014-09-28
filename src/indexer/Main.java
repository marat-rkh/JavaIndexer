package indexer;

import indexer.exceptions.IndexClosedException;
import indexer.tokenizer.Word;
import indexer.tokenizer.WordsTokenizer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

/**
 * Created by mrx on 27.09.14.
 */
public class Main {
    public static void main(String[] args) {
        showHelp();
        try (FSIndexer fsIndexer = new FSIndexer(new WordsTokenizer());
             BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
                String input = br.readLine();
                String[] command = input.split(" ");
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
                    case "h":
                        showHelp();
                        break;
                    case "q":
                        return;
                    default:
                        showUnknownCommandMsg();
                }
            }
        } catch (IOException e) {
            System.out.println("Error occurred. Details: " + e.getMessage());
        } catch (IndexClosedException e) {
            System.out.println("Strange situation - index has been closed");
        }
    }

    private static void showHelp() {
        final String help = "Commands:\n" +
                "a <file_or_dir_path> - add file or dir to index" +
                "r <file_or_dir_path> - remove file or dir from index" +
                "s <word>             - get list of files containing <word>" +
                "c <file_path>        - check if index contains file" +
                "h                    - show this help" +
                "q                    - finish work";
        System.out.println(help);
    }

    private static void addCommand(FSIndexer fsIndexer, String file) throws IndexClosedException {
        fsIndexer.add(file);
    }

    private static void removeCommand(FSIndexer fsIndexer, String file) throws IndexClosedException {
        fsIndexer.remove(file);
    }

    private static void searchCommand(FSIndexer fsIndexer, String what) throws IndexClosedException {
        List<String> files = fsIndexer.search(new Word(what));
        for(String f : files) {
            System.out.println(f);
        }
    }

    private static void containsCommand(FSIndexer fsIndexer, String file) throws IndexClosedException {
        System.out.println(fsIndexer.containsFile(file));
    }

    private static void showUnknownCommandMsg() {
        System.out.println("Unknown command");
    }
}
