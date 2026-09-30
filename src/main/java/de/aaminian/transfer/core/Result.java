package de.aaminian.transfer.core;

import java.util.function.Function;

public sealed interface Result<T> {
    record Success<T>(T value) implements Result<T> {}
    record Failure<T>(TransferError error) implements Result<T> {}
    
    static <T> Result<T> success(T value) { return new Success<>(value); }
    static <T> Result<T> failure(TransferError error) { return new Failure<>(error); }
    
    default <U> Result<U> flatMap(Function<? super T, Result<U>> next) {
        return switch (this) {
            case Success<T>(var value) -> next.apply(value);
            case Failure<T>(var error) -> Result.failure(error);
        };
    }
}
