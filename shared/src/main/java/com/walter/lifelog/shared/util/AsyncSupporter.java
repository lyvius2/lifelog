package com.walter.lifelog.shared.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public final class AsyncSupporter {
    private static final Logger log = LoggerFactory.getLogger(AsyncSupporter.class);

    private AsyncSupporter() {}

    public static <T> CompletableFuture<T> asyncSupply(TaskExecutor executor, Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, executor)
                .exceptionally(ex -> {
                    log.error("Async task failed", ex);
                    return null;
                });
    }
}

