package de.aaminian.transfer.service;

import de.aaminian.transfer.core.Result;
import de.aaminian.transfer.core.Transfer;
import de.aaminian.transfer.core.TransferError;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Executes transfers, either synchronously or asynchronously.
 * Thread-safe: holds no mutable state; can be shared across threads.
 */
public final class TransferService implements AutoCloseable {
    
    private final ExecutorService executor;
    private final boolean ownsExecutor;
    
    /** Creates a de.aaminian.transfer.service that runs async transfers on virtual threads. */
    public TransferService() {
        this(Executors.newVirtualThreadPerTaskExecutor(), true);
    }
    
    /** Creates a de.aaminian.transfer.service using a caller-managed executor (not closed by this de.aaminian.transfer.service). */
    public TransferService(ExecutorService executor) {
        this(executor, false);
    }
    
    private TransferService(ExecutorService executor, boolean ownsExecutor) {
        this.executor = Objects.requireNonNull(executor, "executor");
        this.ownsExecutor = ownsExecutor;
    }
    
    /** Runs the transfer on the calling thread. Never throws; failures are returned. */
    public <T> Result<Long> run(Transfer<T> transfer) {
        Objects.requireNonNull(transfer, "transfer");
        try {
            return transfer.source().read()
                           .flatMap(items -> transfer.target().write(items));
        } catch (RuntimeException e) {
            return Result.failure(new TransferError.UnexpectedFailure(
                "Transfer '%s' failed unexpectedly: %s".formatted(transfer.name(), e),
                e));
        }
    }
    
    /** Runs the transfer asynchronously. The future always completes normally with a Result. */
    public <T> CompletableFuture<Result<Long>> submit(Transfer<T> transfer) {
        Objects.requireNonNull(transfer, "transfer");
        return CompletableFuture.supplyAsync(() -> run(transfer), executor);
    }
    
    @Override
    public void close() {
        if (ownsExecutor) {
            executor.close();
        }
    }
}
