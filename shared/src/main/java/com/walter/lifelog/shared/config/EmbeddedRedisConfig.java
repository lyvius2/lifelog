package com.walter.lifelog.shared.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import redis.embedded.RedisServer;

@Configuration
@Profile("!live")
public class EmbeddedRedisConfig {
    private static final Logger log = LoggerFactory.getLogger(EmbeddedRedisConfig.class);
    private RedisServer redisServer;

    @PostConstruct
    public void startRedis() {
        try {
            redisServer = RedisServer.newRedisServer().build();
            redisServer.start();
            log.info("Embedded Redis started");
        } catch (Exception e) {
            log.error("Embedded Redis started on failed {}", e.getMessage());
        }
    }

    @PreDestroy
    public void stopRedis() {
        try {
            if (redisServer != null) {
                redisServer.stop();
                log.info("Embedded Redis stopped");
            }
        } catch (Exception e) {
            log.warn("Embedded Redis stopped on failed {}", e.getMessage());
        }
    }
}
