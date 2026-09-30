package de.aaminian.transfer.core;

import java.util.Objects;

public record Transfer<T>(String name, Source<? extends T> source, Target<? super T> target) {
    public Transfer {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
    }
}
