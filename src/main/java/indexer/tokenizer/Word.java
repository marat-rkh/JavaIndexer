package indexer.tokenizer;

/**
 * Created by mrx on 26.09.14.
 */
public class Word implements Token {
    private final String value;

    public Word(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Word)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        return value.equals(((Word) obj).getValue());
    }

    @Override
    public int hashCode() {
        return 31 * (value == null ? 0 : value.hashCode());
    }
}
