package de.aaminian.transfer.connector;

import de.aaminian.transfer.core.Result;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConsoleTargetTest {

    @Test
    void printsEachElementWithPrefixAndReturnsCount() {
        var buffer = new ByteArrayOutputStream();
        var target = new ConsoleTarget(new PrintStream(buffer, true, StandardCharsets.UTF_8), "> ");

        var result = target.write(List.of("a", 1, true).iterator());

        assertEquals(Result.success(3L), result);
        assertEquals(List.of("> a", "> 1", "> true"),
                buffer.toString(StandardCharsets.UTF_8).lines().toList());
    }
}