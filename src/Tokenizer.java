import java.io.Reader;
import java.util.List;

/**
 * Created by mrx on 26.09.14.
 */
public interface Tokenizer {
    public List<Token> tokenize(Reader reader);
}
