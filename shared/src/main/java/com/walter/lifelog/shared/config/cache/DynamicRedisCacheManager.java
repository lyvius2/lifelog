package com.walter.lifelog.shared.config.cache;

import org.jetbrains.annotations.NotNull;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import java.time.Duration;

public class DynamicRedisCacheManager extends RedisCacheManager {
    private final RedisCacheConfiguration defaultConfig;
    private final DynamicCacheRegistry dynamicCacheRegistry;

    public DynamicRedisCacheManager(RedisConnectionFactory connectionFactory, RedisCacheConfiguration defaultConfig, DynamicCacheRegistry dynamicCacheRegistry) {
        super(RedisCacheWriter.nonLockingRedisCacheWriter(connectionFactory), defaultConfig);
        this.defaultConfig = defaultConfig;
        this.dynamicCacheRegistry = dynamicCacheRegistry;
    }

    @NotNull
    @Override
    protected RedisCache createRedisCache(@NotNull String name, RedisCacheConfiguration cacheConfig) {
        final Duration ttl = dynamicCacheRegistry.getTtl(name);
        if (ttl != null) {
            cacheConfig = defaultConfig.entryTtl(ttl);
        }
        return super.createRedisCache(name, cacheConfig);
    }
}
