package indexer.encoding.automaton;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Automaton for charset that consumes bytes and analyzing them changes it's state as follows:
 * if all consumed bytes are valid for specified encoding and probability of all consumed bytes
 * to be encoded in specified encoding is higher than threshold automaton stays in OK state,
 * else - state is changed to ERROR and leaves so until reset() method is called.
 * Probability is estimated by instance of ControlsCounter class.
 *
 * @see indexer.encoding.automaton.ControlsCounter
 */
public class EncodingAutomaton {
    private final EncodingValidator validator;
    private final ControlsCounter controlsCounter = new ControlsCounter();
    private final Charset charset;

    private State state = State.OK;

    private long expectedBytesNumber = -1;
    private final double confidenceThreshold;

    public EncodingAutomaton(Charset charset, int bytesBufferSize, double confidenceThreshold) {
        this.charset = charset;
        this.validator = new EncodingValidator(charset, bytesBufferSize);
        this.confidenceThreshold = confidenceThreshold;
    }

    public void feed(ByteBuffer bytes, boolean isEnd) {
        if(!state.equals(State.ERROR)) {
            CharArrayWrapper decodedChars = validator.feed(bytes, isEnd);
            if(validator.getState().equals(EncodingValidator.State.ERROR)) {
                state = State.ERROR;
                return;
            }
            controlsCounter.feed(decodedChars);
            if (!tryCheckConfidenceInAdvance()) {
                return;
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
