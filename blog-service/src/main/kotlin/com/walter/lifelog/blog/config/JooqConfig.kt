package com.walter.lifelog.blog.config

import org.springframework.boot.jooq.autoconfigure.DefaultConfigurationCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JooqConfig {
    @Bean
    fun jooqConfigurationCustomizer(): DefaultConfigurationCustomizer {
        return DefaultConfigurationCustomizer { config ->
            config.settings()
                .withExecuteLogging(true)
                .withRenderFormatted(true)
        }
    }
}

