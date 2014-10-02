package indexer.tokenizer;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mrx on 26.09.14.
 */
public class WordsTokenizer implements Tokenizer {
    private final StringBuilder stringBuilder = new StringBuilder();
    private int symbol;

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
        symbol = reader.read();
        while(((char)symbol) != ' ' && ((char)symbol) != '\n' && symbol != -1) {
            stringBuilder.append((char)symbol);
            symbol = reader.read();
        }
        String word = stringBuilder.toString().intern();
        stringBuilder.setLength(0);
        if(symbol == -1 && word.equals("")) {
            return null;
        }
        return new Word(word);
    }
}
