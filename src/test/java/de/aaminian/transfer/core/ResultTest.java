package de.aaminian.transfer.core;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ResultTest {
    
    @Test
    void flatMapChainsOnSuccess() {
        Result<Integer> result = Result.success(2);
        
        assertEquals(Result.success(4), result.flatMap(x -> Result.success(x * 2)));
    }
    
    @Test
    void flatMapShortCircuitsOnFailure() {
        var error = new TransferError.ReadFailed("down");
        Result<Integer> result = Result.failure(error);
        var called = new AtomicBoolean(false);
        
        var next = result.flatMap(x -> {
            called.set(true);
            return Result.success(x);
        });
        
        assertEquals(Result.failure(error), next);
        assertFalse(called.get(), "next step must not run after a failure");
    }
}