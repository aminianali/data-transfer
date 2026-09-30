package core;

public sealed interface TransferError {
    String message();
    
    record ReadFailed(String message) implements TransferError {}
    record WriteFailed(String message) implements TransferError {}
}
