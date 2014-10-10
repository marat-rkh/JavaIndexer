package indexer.encoding;

import indexer.encoding.automaton.Automaton;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by mrx on 08.10.14.
 */
// todo: remove assertions
public class EncodingDetector {
    private final List<Automaton> automata;

    private final ByteBuffer bytes;

    private static final double CONFIDENCE_THRESHOLD = 0.95;

    private EncodingDetector(List<Automaton> automata, int readPortionSize) {
        this.automata = automata;
        this.bytes = ByteBuffer.allocateDirect(readPortionSize);
    }

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
            Automaton bestFit = bestFitAutomaton();
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
//        Iterator<Automaton> it = automata.iterator();
//        bytes.flip();
//        while (it.hasNext()) {
//            Automaton automaton = it.next();
//            automaton.feed(bytes, isEnd);
//            bytes.rewind();
//        }
//
        bytes.flip();
        for(Automaton a : automata) {
            a.feed(bytes, isEnd);
            bytes.rewind();
        }
    }

    private Automaton bestFitAutomaton() {
//            Iterator<Automaton> it = automata.iterator();
//            Automaton best = null;
//            while (it.hasNext()) {
//                Automaton current = it.next();
//                double bestConfidence = best == null ? -1 : best.getConfidence();
//                if(!current.getState().equals(Automaton.State.ERROR) && current.getConfidence() > bestConfidence) {
//                    best = current;
//                }
//            }
//            return best;

        Automaton best = null;
        for(Automaton current : automata) {
            double bestConfidence = best == null ? -1 : best.getConfidence();
            if(!current.getState().equals(Automaton.State.ERROR) && current.getConfidence() > bestConfidence) {
                best = current;
            }
        }
        return best;
    }

    private void prepareAutomata(long fileSize) {
        for(Automaton a : automata) {
            a.reset();
            a.setExpectedBytesNumber(fileSize);
        }
        bytes.clear();
    }

    public static EncodingDetector standardDetector() {
        int readPortionSize = 2 * 1024;
        List<Automaton> automata = new LinkedList<>();
        automata.add(new Automaton(StandardCharsets.UTF_8, readPortionSize, CONFIDENCE_THRESHOLD));
        automata.add(new Automaton(StandardCharsets.US_ASCII, readPortionSize, CONFIDENCE_THRESHOLD));
        automata.add(new Automaton(StandardCharsets.ISO_8859_1, readPortionSize, CONFIDENCE_THRESHOLD));
        automata.add(new Automaton(StandardCharsets.UTF_16, readPortionSize, CONFIDENCE_THRESHOLD));
        automata.add(new Automaton(StandardCharsets.UTF_16BE, readPortionSize, CONFIDENCE_THRESHOLD));
        automata.add(new Automaton(StandardCharsets.UTF_16LE, readPortionSize, CONFIDENCE_THRESHOLD));
        return new EncodingDetector(automata, readPortionSize);
    }
}
