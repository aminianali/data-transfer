package connector;

import core.Result;
import core.Target;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.Objects;

/**
 * Prints every element on its own line. Accepts any element type.
 * Thread-safe: each line is written atomically; lines from concurrent transfers may interleave.
 */
public final class ConsoleTarget implements Target<Object> {
    
    private final PrintStream out;
    private final String prefix;
    
    public ConsoleTarget() {
        this(System.out, "");
    }
    
    public ConsoleTarget(String prefix) {
        this(System.out, prefix);
    }
    
    public ConsoleTarget(PrintStream out, String prefix) {
        this.out = Objects.requireNonNull(out, "out");
        this.prefix = Objects.requireNonNull(prefix, "prefix");
    }
    
    @Override
    public Result<Long> write(Iterator<?> items) {
        long count = 0;
        while (items.hasNext()) {
            out.println(prefix + items.next());
            count++;
        }
        return Result.success(count);
    }
}
