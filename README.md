# data-transfer

A small, dependency-free Java library for transferring data between pluggable data stores.

Generic **sources** and **targets** are implemented by **connectors**; a connector can be a
source, a target, or both. A **transfer** plugs a source into a target. A **service** executes
transfers — synchronously or concurrently — and returns the number of transferred elements,
or a typed error if the transfer fails.

## Requirements

- JDK 21+ (built and tested with JDK 25)
- Maven 3.9+

## Build, test, run

```bash
# run all tests
./mvn clean test

# run the sample in Main
./mvn -q compile
java -cp target/classes de.aaminian.transfer.Main
```

## Project structure

```
de.aaminian.transfer
├── core        Domain model — no dependencies on other packages
│   ├── Source<T>, Target<T>   Data store contracts (functional interfaces)
│   ├── Transfer<T>            Immutable definition: source + target
│   ├── Result<T>              Success | Failure (sealed)
│   └── TransferError          ReadFailed | WriteFailed | UnexpectedFailure (sealed)
├── connector   Implementations: InMemoryConnector, ConsoleTarget, FailingSource
├── service     TransferService — executes transfers (sync + async)
└── Main        Sample definitions and execution
```

## Usage

```java
var source = new InMemoryConnector<>(List.of("alice", "bob", "carol"));
var target = new ConsoleTarget("[users] ");

try (var service = new TransferService()) {
    Result<Long> result = service.run(new Transfer<String>("users", source, target));

    switch (result) {
        case Result.Success<Long>(var count) -> System.out.println(count + " element(s) transferred");
        case Result.Failure<Long>(var error) -> System.out.println("Failed: " + error.message());
    }
}
```

Asynchronous execution:

```java
CompletableFuture<Result<Long>> future = service.submit(transfer);
```

## Design decisions

- **Source and target are separate interfaces.** Read-only stores never implement `write`;
  a connector that is both simply implements both (`InMemoryConnector`).
- **Failures are values.** `Result` makes failure explicit in every signature. `flatMap`
  chains read → write and short-circuits on the first failure. Unexpected exceptions thrown
  by connectors are caught and wrapped as `UnexpectedFailure` (cause preserved), so one faulty
  connector never breaks the caller.
- **Sealed types and pattern matching.** Error handling is exhaustive and compiler-checked;
  adding a new error case forces every `switch` to handle it.
- **Lazy streaming.** Elements flow through an `Iterator`, so large sources don't have to fit
  in memory.
- **Flexible generics (PECS).** `Transfer<T>` accepts `Source<? extends T>` and
  `Target<? super T>` — e.g. an `Integer` source can feed an `Object` target such as
  `ConsoleTarget`.
- **Definition vs. execution.** `Transfer` is immutable data; `TransferService` runs it.
- **Zero runtime dependencies.** The core is framework-agnostic and can be wired into any
  application (e.g. Spring) later.

## Concurrency

- Core types are immutable and therefore thread-safe.
- Connectors shared between concurrent transfers must be thread-safe (documented contract on
  `Source` and `Target`). Iterators returned by `read()` are used by a single transfer and need
  not be thread-safe.
- `InMemoryConnector` uses a lock-free `ConcurrentLinkedQueue`; reads work on snapshots.
- `TransferService.submit()` runs transfers asynchronously, by default on virtual threads,
  which suits I/O-bound workloads. The returned future always completes normally with a
  `Result`.
- A caller-provided executor is never closed by the service; the default executor is closed
  by `close()` (the service is `AutoCloseable`).

## Testing

JUnit 5 tests cover:

- successful transfers and element counts
- read and write failures (including that the target is never called after a read failure)
- unexpected exceptions wrapped with their cause
- generic variance (an `Integer` source into an `Object` target)
- concurrent writes into a shared connector under contention — no lost elements
- many transfers submitted concurrently into a shared target
- executor ownership

## Limitations and next steps

- Concurrent writes into the same target interleave; batches are not atomic.
- No timeouts, cancellation, or retries yet (`CompletableFuture.orTimeout`, interrupt checks,
  retry policy with backoff for transient errors).
- No resource-backed connectors (files, databases). These would need `AutoCloseable`
  iterators or a callback-style `read` to guarantee cleanup.
- Element transformation between source and target (e.g. a mapping decorator).
- Distributed execution: `Transfer` definitions could be serialized to a queue and executed by
  worker nodes; this requires idempotent targets and checkpointing.
