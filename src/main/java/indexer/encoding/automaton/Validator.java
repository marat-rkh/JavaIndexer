package indexer.encoding.automaton;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;

/**
 * Created by mrx on 09.10.14.
 */
public class Validator {
    private final CharsetDecoder decoder;
    private final ByteBuffer localBuffer;
    private final byte[] exchangeBuffer;
    private final CharBuffer chars;
    private final CharArrayWrapper charsArrayWrapper;

    private State state = State.OK;

    public Validator(Charset charset, int bytesBufferSize) {
        decoder = charset.newDecoder();
        localBuffer = ByteBuffer.allocate(2 * bytesBufferSize);
        exchangeBuffer = new byte[bytesBufferSize];
        chars = CharBuffer.allocate(bytesBufferSize);
        charsArrayWrapper = new CharArrayWrapper(chars.array());
    }

    public CharArrayWrapper feed(ByteBuffer bytes, boolean isEnd) {
        if(!state.equals(State.ERROR)) {
            putToLocalBuffer(bytes);
            if(!decode(isEnd)) {
                return null;
            }
            fillArrayWithDecodedChars();
            localBuffer.compact();
            return charsArrayWrapper;
        }
        return null;
    }

    private boolean decode(boolean isEnd) {
        CoderResult result = decoder.decode(localBuffer, chars, isEnd);
        if (result.isError()) {
            state = State.ERROR;
            return false;
        }
        if(isEnd) {
            decoder.flush(chars);
        }
        return true;
    }

    private void fillArrayWithDecodedChars() {
        chars.flip();
        int offs = chars.position();
        int len = chars.remaining();
        charsArrayWrapper.setRange(offs, len);
        chars.clear();
    }

    private void putToLocalBuffer(ByteBuffer bytes) {
        int availableBytes = bytes.remaining();
        bytes.get(exchangeBuffer, 0, availableBytes);
        localBuffer.put(exchangeBuffer, 0, availableBytes);
        localBuffer.flip();
    }

    public void reset() {
        decoder.reset();
        chars.clear();
        state = State.OK;
        localBuffer.clear();
    }

    public State getState() {
        return state;
    }

    public enum State {
        ERROR, OK
    }
}
