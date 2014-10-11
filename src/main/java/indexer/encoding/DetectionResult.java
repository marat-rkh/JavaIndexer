package indexer.encoding;

import java.nio.charset.Charset;

/**
 * Represents encoding detection result containing charset and confidence level (level of accuracy)
 *
 * @see indexer.encoding.EncodingDetector
 */
public class DetectionResult {
    private final Charset charset;
    private final double confidence;

    public DetectionResult(Charset charset, double confidence) {
        this.charset = charset;
        this.confidence = confidence;
    }

    public Charset getCharset() {
        return charset;
    }

    public double getConfidence() {
        return confidence;
    }
}
