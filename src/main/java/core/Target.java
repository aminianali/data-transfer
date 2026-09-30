package core;

import java.util.Iterator;

public interface Target<T> {
    Result<Long> write(Iterator<? extends T> data);
}
