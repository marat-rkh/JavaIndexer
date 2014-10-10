package indexer.encoding.automaton;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Created by mrx on 09.10.14.
 */
public class Automaton {
    private final Validator validator;
    private final ControlsCounter controlsCounter = new ControlsCounter();
    private final Charset charset;

    private State state = State.OK;

    private long expectedBytesNumber = -1;
    private final double confidenceThreshold;

    public Automaton(Charset charset, int bytesBufferSize, double confidenceThreshold) {
        this.charset = charset;
        this.validator = new Validator(charset, bytesBufferSize);
        this.confidenceThreshold = confidenceThreshold;
    }

    public void feed(ByteBuffer bytes, boolean isEnd) {
        if(!state.equals(State.ERROR)) {
            CharArrayWrapper decodedChars = validator.feed(bytes, isEnd);
            if(validator.getState().equals(Validator.State.ERROR)) {
                state = State.ERROR;
                return;
            }
            controlsCounter.feed(decodedChars);
            if (!tryCheckConfidenceInAdvance()) {
                return;
            }
            if(isEnd) {
                assert(!controlsCounter.hasPrev());
            }
        }
    }

    private boolean tryCheckConfidenceInAdvance() {
        if(expectedBytesNumber != -1) {
            double currentConfidence = 1 - controlsCounter.getControlsNum() * 1.0 / expectedBytesNumber;
            if (currentConfidence < confidenceThreshold) {
                state = State.ERROR;
                return false;
            }
        }
        return true;
    }

    public State getState() {
        return state;
    }

    public double getConfidence() {
        return 1 - controlsCounter.getPercentage();
    }

    public Charset getCharset() {
        return charset;
    }

    public void reset() {
        validator.reset();
        controlsCounter.reset();
        state = State.OK;
        expectedBytesNumber = -1;
    }

    public void setExpectedBytesNumber(long expectedBytesNumber) {
        this.expectedBytesNumber = expectedBytesNumber;
    }

    public enum State {
        ERROR, OK
    }
}
