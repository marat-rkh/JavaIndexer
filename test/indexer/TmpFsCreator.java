package indexer;

import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by mrx on 28.09.14.
 */
public class TmpFsCreator {
    protected File file1;
    protected File file2;
    protected File file3;
    protected File dir1;
    protected File dir1SubFile1;
    protected File dir2;
    protected File dir2SubFile1;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void create() throws IOException {
        file1 = createFileInTmp("file1", "file1 content");
        file2 = createFileInTmp("file2", "file2 content");
        file3 = createFileInTmp("file3", "file3 content");
        dir1 = tempFolder.newFolder("Dir1");
        dir1SubFile1 = File.createTempFile("dir1SubFile1", "", dir1);
        dir2 = tempFolder.newFolder("Dir2");
        dir2SubFile1 = File.createTempFile("dir2SubFile1", "", dir2);
        if(!appendTextToFile(dir2SubFile1, "Lorem ipsum dolor sit amet, consectetur adipiscing elit")) {
            throw new IOException("append text failed while initializing test files");
        }
    }

    protected File createFileInTmp(String fileName, String content) {
        try {
            File tmpFile = tempFolder.newFile(fileName);
            return appendTextToFile(tmpFile, content) ? tmpFile : null;
        } catch (IOException e) {
            return null;
        }
    }

    protected boolean appendTextToFile(File file, String contentToAppend) {
        return writeTextToFile(file, contentToAppend, true);
    }

    protected boolean rewriteFileWithText(File file, String contentToAppend) {
        return writeTextToFile(file, contentToAppend, false);
    }

    protected boolean writeTextToFile(File file, String contentToAppend, boolean append) {
        try {
            FileWriter fileWriter = new FileWriter(file.getAbsoluteFile(), append);
            fileWriter.write(contentToAppend);
            fileWriter.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
