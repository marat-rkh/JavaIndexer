package indexer.tokenizer;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mrx on 26.09.14.
 */
public class WordsTokenizer implements Tokenizer {
    @Override
    public List<Token> tokenize(Reader reader) throws IOException {
        List<Token> tokens = new ArrayList<Token>();
        Token word = readWord(reader);
        while (word != null) {
            tokens.add(word);
            word = readWord(reader);
        }
        return tokens;
    }

    private Token readWord(Reader reader) throws IOException {
        String wordString = "";
        int data = reader.read();
        while(((char)data) != ' ' && data != -1) {
            wordString += (char)data;
            data = reader.read();
        }
        if(data == -1 && wordString == "") {
            return null;
        }
        return new Word(wordString);
    }
}
