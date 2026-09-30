package core;

import java.util.Iterator;

public interface Source<T> {
    Result<Iterator<T>> read();
}
