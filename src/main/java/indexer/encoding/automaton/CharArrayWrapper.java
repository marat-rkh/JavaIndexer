package indexer.encoding.automaton;

/**
 * Created by mrx on 10.10.14.
 */
public class CharArrayWrapper {
    private final char[] chars;
    private int start;
    private int len;

    public CharArrayWrapper(char[] chars) {
        this.chars = chars;
        this.start = 0;
        this.len = chars.length;
    }

    public void setRange(int start, int len) {
        this.start = start;
        this.len = len;
    }

    public int getLen() {
        return len;
    }

    public char get(int i) {
        return chars[start + i];
    }
}
