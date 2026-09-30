package de.aaminian.transfer.connector;

import de.aaminian.transfer.core.Result;
import de.aaminian.transfer.core.Source;
import de.aaminian.transfer.core.Target;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * An in-memory store that is both a source and a target.
 * Thread-safe: concurrent reads and writes are allowed.
 */
public final class InMemoryConnector<T> implements Source<T>, Target<T> {

    private final Queue<T> items = new ConcurrentLinkedQueue<>();

    public InMemoryConnector() {
    }

    public InMemoryConnector(Collection<? extends T> initial) {
        items.addAll(initial);
    }

    @Override
    public Result<Iterator<T>> read() {
        return Result.success(List.copyOf(items).iterator());
    }

    @Override
    public Result<Long> write(Iterator<? extends T> incoming) {
        long count = 0;
        while (incoming.hasNext()) {
            items.add(incoming.next());
            count++;
        }
        return Result.success(count);
    }

    /** Returns a snapshot of the current contents. */
    public List<T> items() {
        return List.copyOf(items);
    }
}