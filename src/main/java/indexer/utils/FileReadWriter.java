package indexer.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by mrx on 04.10.14.
 */
public class FileReadWriter implements ReadWriter {
    private BufferedReader br = null;

    public FileReadWriter(String commandsFile) throws Exception {
        this.br = new BufferedReader(new FileReader(commandsFile));
    }
    @Override
    public String readLine() throws IOException {
        System.out.print("\n$Enter command: ");
        return br.readLine();
    }
    @Override
    public void println(String msg) {
        System.out.println(msg);
    }
    @Override
    public void print(String msg) throws IOException {
        System.out.print(msg);
    }
    @Override
    public void close() throws Exception {
        if(br != null) {
            br.close();
        }
    }
}
