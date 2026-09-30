package core;

import java.util.Iterator;

/**
 * A data store that consumes elements.
 * <p>
 * Implementations shared between concurrently running transfers must be thread-safe.
 *
 * @param <T> element type
 */
@FunctionalInterface
public interface Target<T> {
    
    /** Writes all elements and returns how many were written, or a failure. */
    Result<Long> write(Iterator<? extends T> items);
}