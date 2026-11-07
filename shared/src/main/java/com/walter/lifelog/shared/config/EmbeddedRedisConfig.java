package com.walter.lifelog.shared.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import redis.embedded.RedisServer;

/**
 * 로컬 개발용 임베디드 Redis.
 *
 * <p>embedded-redis 는 운영 이미지에 넣지 않습니다(11MB). 그래서 라이브러리가 클래스패스에
 * 없을 수도 있으며, {@code @ConditionalOnClass} 가 그 경우 이 설정을 조용히 건너뜁니다.
 * 조건 평가는 ASM 으로 애노테이션만 읽어 수행되므로 클래스 로딩이 일어나지 않습니다.
 */
@Configuration
@Profile("!live")
@ConditionalOnClass(RedisServer.class)
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
