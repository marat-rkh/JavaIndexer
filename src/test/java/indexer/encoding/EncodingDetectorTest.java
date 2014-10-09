package indexer.encoding;

import indexer.TmpFsCreator;
import org.junit.Test;

import static org.junit.Assert.*;

public class EncodingDetectorTest extends TmpFsCreator {
    @Test
    public void testDetect() throws Exception {
        EncodingDetector detector = EncodingDetector.standardDetector();
        assertTrue(detector.detect(dir1SubFile1.getAbsolutePath()) == null);
        assertTrue(detector.detect(dir2SubFile1.getAbsolutePath()) != null);
        assertTrue(detector.detect(file1.getAbsolutePath()) != null);
        assertTrue(detector.detect(file2.getAbsolutePath()) != null);
        assertTrue(detector.detect(file3.getAbsolutePath()) != null);
    }
}