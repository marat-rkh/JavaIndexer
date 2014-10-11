package indexer.encoding;

import indexer.encoding.automaton.EncodingAutomaton;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

/**
 * Simple file encodings detector. Detects standard JVM encodings from StandardCharset factory.
 * If file is not textual or encoding is not supported detector will return null.
 * EncodingDetector reads passed file and sends read bytes to automata representing encodings.
 * At the end it chose the automaton that has OK state and highest confidence level.
 *
 * @see java.nio.charset.StandardCharsets
 * @see indexer.encoding.automaton.EncodingAutomaton
 */
public class EncodingDetector {
    private final List<EncodingAutomaton> automata;

    private final ByteBuffer bytes;

    private static final double CONFIDENCE_THRESHOLD = 0.95;

    private EncodingDetector(List<EncodingAutomaton> automata, int readPortionSize) {
        this.automata = automata;
        this.bytes = ByteBuffer.allocateDirect(readPortionSize);
    }

    /**
     * Detect encoding for specified file.
     *
     * @param filePath
     * @return DetectionResult with encoding and detection confidence level or null
     *         if encoding is not detected or file is not textual
     * @throws IOException
     */
    public DetectionResult detect(String filePath) throws IOException {
        long fileSize = new File(filePath).length();
        if(fileSize == 0) {
            return null;
        }
        prepareAutomata(fileSize);
        ReadableByteChannel inChannel = null;
        try {
            inChannel = new FileInputStream(filePath).getChannel();
            while(inChannel.read(bytes) != -1) {
                if(!doAutomataIteration(false)) {
                    return null;
                }
            }
            if(bytes.position() != 0 && !doAutomataIteration(true)) {
                return null;
            }
            EncodingAutomaton bestFit = bestFitAutomaton();
            return bestFit == null ? null : new DetectionResult(bestFit.getCharset(), bestFit.getConfidence());
        } finally {
            if(inChannel != null) {
                inChannel.close();
            }
        }
    }

    private boolean doAutomataIteration(boolean isEnd) {
        feedAutomata(isEnd);
        bytes.clear();
        return true;
    }

    private void feedAutomata(boolean isEnd) {
        bytes.flip();
        for(EncodingAutomaton a : automata) {
            a.feed(bytes, isEnd);
            bytes.rewind();
        }
    }

    private EncodingAutomaton bestFitAutomaton() {
        EncodingAutomaton best = null;
        for(EncodingAutomaton current : automata) {
            double bestConfidence = best == null ? -1 : best.getConfidence();
            if(!current.getState().equals(EncodingAutomaton.State.ERROR) && current.getConfidence() > bestConfidence) {
                best = current;
            }
        }
        return best;
    }

    private void prepareAutomata(long fileSize) {
        for(EncodingAutomaton a : automata) {
            a.reset();
            a.setExpectedBytesNumber(fileSize);
        }
        bytes.clear();
    }

    public static EncodingDetector standardDetector() {
        int readPortionSize = 2 * 1024;
        List<EncodingAutomaton> automata = new LinkedList<>();
        automata.add(new EncodingAutomaton(StandardCharsets.UTF_8, readPortionSize, CONFIDENCE_THRESHOLD));
        automata.add(new EncodingAutomaton(StandardCharsets.US_ASCII, readPortionSize, CONFIDENCE_THRESHOLD));
        automata.add(new EncodingAutomaton(StandardCharsets.ISO_8859_1, readPortionSize, CONFIDENCE_THRESHOLD));
        automata.add(new EncodingAutomaton(StandardCharsets.UTF_16, readPortionSize, CONFIDENCE_THRESHOLD));
        automata.add(new EncodingAutomaton(StandardCharsets.UTF_16BE, readPortionSize, CONFIDENCE_THRESHOLD));
        automata.add(new EncodingAutomaton(StandardCharsets.UTF_16LE, readPortionSize, CONFIDENCE_THRESHOLD));
        return new EncodingDetector(automata, readPortionSize);
    }
}
