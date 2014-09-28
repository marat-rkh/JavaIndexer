package indexer;

import indexer.fsmonitor.FSMonitorsManager;
import indexer.handler.IndexEventsHandler;
import indexer.handler.IndexUpdater;
import indexer.index.ConcurrentHashFileIndex;
import indexer.index.FileIndex;
import indexer.tokenizer.Word;
import indexer.tokenizer.WordsTokenizer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Created by mrx on 27.09.14.
 */
public class Main {
    public static void main(String[] args) {
        FSIndexer fsIndexer = new FSIndexer(new WordsTokenizer());
        try {
            fsIndexer.add("/home/mrx/Test");
            System.out.println("Added");

            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            br.readLine();
            System.out.println("Done");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            fsIndexer.close();
        }
//        try {
//            Files.walkFileTree(Paths.get("/home/mrx/Test"), new SimpleFileVisitor<Path>() {
//                @Override
//                public FileVisitResult visitFile(Path dir, BasicFileAttributes attrs) throws IOException {
//                    System.out.println(dir.toFile().getAbsolutePath());
//                    return FileVisitResult.CONTINUE;
//                }
//            });
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        Path p1 = Paths.get("/home/mrx/");
//        Path p2 = Paths.get("/home/mrx/t");
//        System.out.println(p2.startsWith(p1));
    }

    private static FileIndex fileIndex = new ConcurrentHashFileIndex(new WordsTokenizer());
    private static IndexEventsHandler indexUpdater = new IndexUpdater(fileIndex);

    private static IndexEventsHandler eventsPrinter = new IndexEventsHandler() {
        @Override
        public void onFilesAddedEvent(Path filePath) {
            System.out.println("onCreateFile: " + filePath.toString());
        }
        @Override
        public void onFilesRemovedEvent(Path filePath) {
            System.out.println("onRemoveFile: " + filePath.toString());
        }
        @Override
        public void onFilesModifiedEvent(Path filePath) {
            System.out.println("onModifyFile: " + filePath.toString());
        }
    };
}
