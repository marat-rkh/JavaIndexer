package indexer.encoding.automaton;

/**
 * Created by mrx on 09.10.14.
 */
public class ControlsCounter {
    private char prev;
    private boolean hasPrev = false;
    private char[] convertingBuffer = new char[2];

    private int allChars = 0;
    private int controlsNum = 0;

    public void feed(CharArrayWrapper chars) {
        assert(chars.getLen() != 0);
        int i = 0;
        if(hasPrev) {
            i = 1;
            count(codePointForPair(prev, chars.get(0)));
            hasPrev = false;
        }
        for(; i < chars.getLen(); i++) {
            if(!Character.isHighSurrogate(chars.get(i))) {
                convertingBuffer[0] = chars.get(i);
                count(Character.codePointAt(convertingBuffer, 0));
            } else {
                if(i != chars.getLen() - 1) {
                    count(codePointForPair(chars.get(i), chars.get(i + 1)));
                    i += 1;
                } else {
                    hasPrev = true;
                    prev = chars.get(i);
                }
            }
        }
    }

    public void reset() {
        hasPrev = false;
        allChars = 0;
        controlsNum = 0;
    }

    public double getPercentage() {
        return controlsNum * 1.0 / allChars;
    }

    public boolean hasPrev() {
        return hasPrev;
    }

    public int getControlsNum() {
        return controlsNum;
    }

    private int codePointForPair(char fst, char snd) {
        assert(!Character.isHighSurrogate(snd));
        convertingBuffer[0] = fst;
        convertingBuffer[1] = snd;
        return Character.codePointAt(convertingBuffer, 0);
    }

    private void count(int codePoint) {
        allChars += 1;
//        assert (Character.isDefined(codePoint) && Character.isValidCodePoint(codePoint));
        if(Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
            controlsNum += 1;
        }
    }
}
