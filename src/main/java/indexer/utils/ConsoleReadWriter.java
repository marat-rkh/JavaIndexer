package indexer.utils;

import jline.console.ConsoleReader;
import jline.console.completer.ArgumentCompleter;
import jline.console.completer.Completer;
import jline.console.completer.FileNameCompleter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mrx on 04.10.14.
 */
public class ConsoleReadWriter implements ReadWriter {
    private ConsoleReader console = null;

    public ConsoleReadWriter() throws Exception {
        console = new ConsoleReader();
        List<Completer> completers = new ArrayList<>();
        completers.add(new FileNameCompleter());
        ArgumentCompleter argumentCompleter = new ArgumentCompleter(completers);
        argumentCompleter.setStrict(false);
        console.addCompleter(argumentCompleter);
        console.setPrompt("\n$Enter command: ");
    }
    @Override
    public String readLine() throws IOException {
        return console.readLine();
    }
    @Override
    public void println(String msg) throws IOException {
        console.println(msg);
        console.flush();
    }
    @Override
    public void print(String msg) throws IOException {
        console.print(msg);
        console.flush();
    }
    @Override
    public void close() throws Exception {
        if(console != null) {
            console.getTerminal().restore();
            console.shutdown();
        }
    }
}