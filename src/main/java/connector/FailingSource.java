package connector;

import core.Result;
import core.Source;
import core.TransferError;

import java.util.Iterator;
import java.util.Objects;

/** A source that always fails. Useful for demos and tests of the failure path. */
public record FailingSource<T>(String reason) implements Source<T> {
    
    public FailingSource {
        Objects.requireNonNull(reason, "reason");
    }
    
    @Override
    public Result<Iterator<T>> read() {
        return Result.failure(new TransferError.ReadFailed(reason));
    }
}
