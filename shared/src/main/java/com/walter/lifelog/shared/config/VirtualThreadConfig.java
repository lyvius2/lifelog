package com.walter.lifelog.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

@Configuration
public class VirtualThreadConfig {
    private static final int MAX_CONCURRENCY = Runtime.getRuntime().availableProcessors() * 2;

    @Bean
    public TaskExecutor virtualThreadExecutor() {
        final Semaphore semaphore = new Semaphore(MAX_CONCURRENCY);
        final Executor base = Executors.newVirtualThreadPerTaskExecutor();

        return task -> base.execute(() -> {
            semaphore.acquireUninterruptibly();
            try {
                task.run();
            } finally {
                semaphore.release();
            }
        });
    }
}
