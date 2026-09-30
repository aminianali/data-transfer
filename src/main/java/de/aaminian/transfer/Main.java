package de.aaminian.transfer;

import de.aaminian.transfer.connector.ConsoleTarget;
import de.aaminian.transfer.connector.FailingSource;
import de.aaminian.transfer.connector.InMemoryConnector;
import de.aaminian.transfer.core.Result;
import de.aaminian.transfer.core.Result.Failure;
import de.aaminian.transfer.core.Result.Success;
import de.aaminian.transfer.core.Source;
import de.aaminian.transfer.core.Transfer;
import de.aaminian.transfer.core.TransferError;
import de.aaminian.transfer.service.TransferService;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("java:S106") // Console output is the purpose of this demo, not logging
public final class Main {
    
    private Main() { }
    
    public static void main(String[] args) {
        // --- 1. Data stores (connectors) ---
        var users = new InMemoryConnector<>(List.of("alice", "bob", "carol"));
        var numbers = new InMemoryConnector<>(List.of(1, 2, 3, 4, 5));
        var archive = new InMemoryConnector<String>();
        Source<String> buggy = () -> {
            throw new IllegalStateException("bug in de.aaminian.transfer.connector");
        };
        
        // --- 2. Transfer definitions ---
        List<Transfer<?>> transfers = List.of(
            new Transfer<String>("users-to-console", users, new ConsoleTarget("[users] ")),
            new Transfer<String>("users-to-archive", users, archive),
            new Transfer<Integer>("numbers-to-console", numbers, new ConsoleTarget("[numbers] ")),
            new Transfer<String>("broken-source", new FailingSource<>("connection refused"), archive),
            new Transfer<String>("buggy-source", buggy, new ConsoleTarget())
        );
        
        try (var service = new TransferService()) {
            // --- 3. Concurrent execution ---
            System.out.println("== Running " + transfers.size() + " transfers concurrently ==");
            
            record Running(String name, CompletableFuture<Result<Long>> result) {}
            
            List<Running> running = transfers.stream()
                                             .map(t -> new Running(t.name(), service.submit(t)))
                                             .toList();
            
            CompletableFuture.allOf(running.stream()
                                           .map(Running::result)
                                           .toArray(CompletableFuture[]::new)).join();
            
            System.out.println("== Results ==");
            running.forEach(r -> report(r.name(), r.result().join()));
            
            // --- 4. The archive is now used as a source ---
            System.out.println("== Archive -> console (synchronous) ==");
            var replay = new Transfer<String>("archive-to-console", archive, new ConsoleTarget("[archive] "));
            report(replay.name(), service.run(replay));
        }
    }
    
    private static void report(String name, Result<Long> result) {
        String line = switch (result) {
            case Success<Long>(var count) -> "OK   %-20s %d element(s)".formatted(name, count);
            case Failure<Long>(var error) -> "FAIL %-20s %s".formatted(name, describe(error));
        };
        System.out.println(line);
    }
    
    private static String describe(TransferError error) {
        return switch (error) {
            case TransferError.ReadFailed(var message) -> "read failed: " + message;
            case TransferError.WriteFailed(var message) -> "write failed: " + message;
            case TransferError.UnexpectedFailure u -> "unexpected: " + u.message();
        };
    }
}
