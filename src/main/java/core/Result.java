package core;

import java.util.function.Function;

public sealed interface Result<T> {
    record Success<T>(T value) implements Result<T> {}
    record Failure<T>(TransferError error) implements Result<T> {}
    
    static <T> Result<T> success(T value) { return new Success<>(value); }
    static <T> Result<T> failure(TransferError error) { return new Failure<>(error); }
    
    default <U> Result<U> flatMap(Function<? super T, Result<U>> next) {
        return switch (this) {
            case Success<T> s -> next.apply(s.value());
            case Failure<T> f -> new Failure<>(f.error());
        };
    }
}
