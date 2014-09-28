package indexer.exceptions;

/**
 * Created by mrx on 28.09.14.
 */
public class IndexClosedException extends Exception {
    public IndexClosedException() {
        super("Some method has been called on closed index");
    }
}
