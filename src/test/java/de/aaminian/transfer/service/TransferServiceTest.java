package de.aaminian.transfer.service;

import de.aaminian.transfer.connector.FailingSource;
import de.aaminian.transfer.connector.InMemoryConnector;
import de.aaminian.transfer.core.Result;
import de.aaminian.transfer.core.Source;
import de.aaminian.transfer.core.Target;
import de.aaminian.transfer.core.Transfer;
import de.aaminian.transfer.core.TransferError;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.fail;

class TransferServiceTest {

    private TransferService service;

    @BeforeEach
    void setUp() {
        service = new TransferService();
    }

    @AfterEach
    void tearDown() {
        service.close();
    }

    @Test
    void successfulTransferReturnsCountAndMovesData() {
        var source = new InMemoryConnector<>(List.of("a", "b", "c"));
        var target = new InMemoryConnector<String>();

        var result = service.run(new Transfer<String>("ok", source, target));

        assertEquals(Result.success(3L), result);
        assertEquals(List.of("a", "b", "c"), target.items());
    }

    @Test
    void readFailureIsReturnedAndTargetIsNeverCalled() {
        var writeCalled = new AtomicBoolean(false);
        Target<String> target = items -> {
            writeCalled.set(true);
            return Result.success(0L);
        };

        var result = service.run(new Transfer<String>("read-fail", new FailingSource<>("down"), target));

        assertEquals(Result.failure(new TransferError.ReadFailed("down")), result);
        assertFalse(writeCalled.get(), "target must not be called when reading fails");
    }

    @Test
    void writeFailureIsReturned() {
        var source = new InMemoryConnector<>(List.of("a"));
        Target<String> full = items -> Result.failure(new TransferError.WriteFailed("disk full"));

        var result = service.run(new Transfer<String>("write-fail", source, full));

        assertEquals(Result.failure(new TransferError.WriteFailed("disk full")), result);
    }

    @Test
    void unexpectedExceptionIsWrappedWithCause() {
        Source<String> buggy = () -> {
            throw new IllegalStateException("boom");
        };

        var result = service.run(new Transfer<String>("buggy", buggy, new InMemoryConnector<>()));

        var error = assertInstanceOf(TransferError.UnexpectedFailure.class, errorOf(result));
        assertInstanceOf(IllegalStateException.class, error.cause());
    }

    @Test
    void integerSourceCanFeedObjectTarget() {
        var numbers = new InMemoryConnector<>(List.of(1, 2, 3));
        var anything = new InMemoryConnector<Object>();

        var result = service.run(new Transfer<Integer>("pecs", numbers, anything));

        assertEquals(Result.success(3L), result);
        assertEquals(List.of(1, 2, 3), anything.items());
    }

    @Test
    void submitRunsManyTransfersConcurrentlyIntoSharedTarget() {
        var source = new InMemoryConnector<>(IntStream.range(0, 100).boxed().toList());
        var target = new InMemoryConnector<Integer>();

        var futures = IntStream.range(0, 50)
                .mapToObj(i -> service.submit(new Transfer<Integer>("t" + i, source, target)))
                .toList();

        futures.forEach(f -> assertEquals(Result.success(100L), f.join()));
        assertEquals(5_000, target.items().size());
    }

    @Test
    void doesNotCloseCallerOwnedExecutor() {
        var executor = Executors.newSingleThreadExecutor();
        try {
            new TransferService(executor).close();

            assertFalse(executor.isShutdown(), "caller-owned executor must stay open");
        } finally {
            executor.shutdownNow();
        }
    }

    private static TransferError errorOf(Result<?> result) {
        return switch (result) {
            case Result.Failure<?>(var error) -> error;
            case Result.Success<?>(var value) -> fail("expected failure but got success: " + value);
        };
    }
}