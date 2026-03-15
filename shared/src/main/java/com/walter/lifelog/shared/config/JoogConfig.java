package com.walter.lifelog.shared.config;

import org.springframework.boot.jooq.autoconfigure.DefaultConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JoogConfig {
    @Bean
    public DefaultConfigurationCustomizer jooqConfigurationCustomizer() {
        return config -> config.settings()
                .withExecuteLogging(true)
                .withRenderFormatted(true);
    }
}
