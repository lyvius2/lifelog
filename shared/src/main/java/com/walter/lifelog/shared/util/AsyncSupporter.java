package com.walter.lifelog.shared.util;

import org.springframework.core.task.TaskExecutor;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public final class AsyncSupporter {

    private AsyncSupporter() {}

    public static <T> CompletableFuture<T> asyncSupply(TaskExecutor executor, Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, executor);
    }
}

