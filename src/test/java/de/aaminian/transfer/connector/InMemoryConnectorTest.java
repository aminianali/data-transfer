package de.aaminian.transfer.connector;

import de.aaminian.transfer.core.Result;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InMemoryConnectorTest {

    @Test
    void readReturnsInitialItems() {
        var connector = new InMemoryConnector<>(List.of("a", "b"));

        var items = new ArrayList<String>();
        switch (connector.read()) {
            case Result.Success<java.util.Iterator<String>>(var iterator) -> iterator.forEachRemaining(items::add);
            case Result.Failure<java.util.Iterator<String>>(var error) -> throw new AssertionError(error.message());
        }

        assertEquals(List.of("a", "b"), items);
    }

    @Test
    void writeAppendsAndReturnsCount() {
        var connector = new InMemoryConnector<>(List.of("a"));

        var result = connector.write(List.of("b", "c").iterator());

        assertEquals(Result.success(2L), result);
        assertEquals(List.of("a", "b", "c"), connector.items());
    }

    @Test
    void readIsASnapshotUnaffectedByLaterWrites() {
        var connector = new InMemoryConnector<>(List.of("a"));
        var snapshot = connector.read();

        connector.write(List.of("b").iterator());

        var items = new ArrayList<String>();
        if (snapshot instanceof Result.Success<java.util.Iterator<String>>(var iterator)) {
            iterator.forEachRemaining(items::add);
        }
        assertEquals(List.of("a"), items);
    }

    @Test
    void concurrentWritesLoseNoElements() throws Exception {
        var connector = new InMemoryConnector<Integer>();
        int writers = 16;
        int perWriter = 10_000;
        var startGate = new CountDownLatch(1);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Result<Long>>> futures = IntStream.range(0, writers)
                    .mapToObj(w -> executor.submit(() -> {
                        startGate.await();
                        return connector.write(IntStream.range(0, perWriter).boxed().iterator());
                    }))
                    .toList();

            startGate.countDown();

            for (var future : futures) {
                assertEquals(Result.success((long) perWriter), future.get());
            }
        }

        assertEquals(writers * perWriter, connector.items().size());
    }
}