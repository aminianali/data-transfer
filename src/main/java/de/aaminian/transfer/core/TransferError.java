package de.aaminian.transfer.core;

public sealed interface TransferError {
    String message();
    
    record ReadFailed(String message) implements TransferError {}
    record WriteFailed(String message) implements TransferError {}
    record UnexpectedFailure(String message, Throwable cause) implements TransferError {}
}
