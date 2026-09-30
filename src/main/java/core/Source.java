package core;

import java.util.Iterator;

/**
 * A data store that produces elements.
 * <p>
 * Implementations shared between concurrently running transfers must be thread-safe.
 * The returned iterator is used by a single transfer and need not be thread-safe.
 *
 * @param <T> element type
 */
@FunctionalInterface
public interface Source<T> {

    /** Opens the source for reading or returns a failure if it can't be read. */
    Result<Iterator<T>> read();
}