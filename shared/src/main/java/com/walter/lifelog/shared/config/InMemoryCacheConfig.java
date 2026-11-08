package com.walter.lifelog.shared.config;

import com.walter.lifelog.shared.config.cache.DynamicCacheRegistry;
import com.walter.lifelog.shared.config.cache.InMemoryDynamicCacheManager;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Duration;

/**
 * live 가 아닌 프로파일의 캐시. Redis 없이 기동하기 위한 것이며 {@link RedisCacheConfig} 와
 * 짝을 이룬다. 둘 중 하나만 활성화된다.
 */
@Configuration
@Profile("!live")
public class InMemoryCacheConfig {

    @Bean
    public CacheManager cacheManager(DynamicCacheRegistry dynamicCacheRegistry) {
        return new InMemoryDynamicCacheManager(dynamicCacheRegistry, Duration.ofHours(24));
    }
}
