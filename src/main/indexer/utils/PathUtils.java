package indexer.utils;

import java.nio.file.Path;

/**
 * Created by mrx on 28.09.14.
 */
public class PathUtils {
    public static boolean pathsAreEqual(Path fst, Path snd) {
        return fst.startsWith(snd) && snd.startsWith(fst);
    }

    public static boolean firstPathIsParent(Path fst, Path snd) {
        return snd.startsWith(fst);
    }
}
