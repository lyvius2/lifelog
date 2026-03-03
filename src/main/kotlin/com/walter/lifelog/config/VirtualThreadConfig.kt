package com.walter.lifelog.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskExecutor
import org.springframework.core.task.support.TaskExecutorAdapter
import java.util.concurrent.Executors

@Configuration
class VirtualThreadConfig {
    @Bean
    fun virtualThreadExecutor(): TaskExecutor {
        return TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor())
    }
}
