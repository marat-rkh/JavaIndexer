package indexer.tokenizer;

import org.junit.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class WordsTokenizerTest {
    @Test
    public void testTokenize() throws Exception {
        Tokenizer wordTokenizer = new WordsTokenizer();
        String input = "First test for tokenizer";
        Reader reader = new StringReader(input);
        List<Token> tokens = null;
        try {
            tokens = wordTokenizer.tokenize(reader);
        } catch (IOException e) {
            fail("IOException has been thrown");
        }
        reader.close();
        List<Word> expected = Arrays.asList(new Word("First"), new Word("test"), new Word("for"), new Word("tokenizer"));
        assertEquals(tokens, expected);
    }
}