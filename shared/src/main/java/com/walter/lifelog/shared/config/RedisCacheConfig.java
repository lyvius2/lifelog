package com.walter.lifelog.shared.config;

import com.walter.lifelog.shared.config.cache.DynamicCacheRegistry;
import com.walter.lifelog.shared.config.cache.DynamicRedisCacheManager;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
public class RedisCacheConfig {
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory, DynamicCacheRegistry dynamicCacheRegistry) {
        final RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(24))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.json()))
                .disableCachingNullValues();
        return new DynamicRedisCacheManager(connectionFactory, defaultConfig, dynamicCacheRegistry);
    }

    @Bean
    public ClientOptions clientOptions() {
        return ClientOptions.builder()
            .socketOptions(
                SocketOptions.builder()
                    .keepAlive(SocketOptions.KeepAliveOptions.builder()
                        .enable(true)
                        .idle(Duration.ofSeconds(60))
                        .interval(Duration.ofSeconds(10))
                        .count(3)
                        .build())
                    .build()
            )
            .build();
    }
}
