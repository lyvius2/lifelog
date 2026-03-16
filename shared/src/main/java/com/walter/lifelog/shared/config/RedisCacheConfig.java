package com.walter.lifelog.shared.config;

import com.walter.lifelog.shared.annotation.DynamicCacheable;
import com.walter.lifelog.shared.config.cache.DynamicCacheRegistry;
import com.walter.lifelog.shared.config.cache.DynamicCacheableInterceptor;
import com.walter.lifelog.shared.config.cache.DynamicRedisCacheManager;
import org.aopalliance.aop.Advice;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
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
@EnableCaching
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
    public DefaultPointcutAdvisor dynamicCacheableAdvisor(CacheManager cacheManager, DynamicCacheRegistry dynamicCacheRegistry) {
        final Pointcut pointcut = AnnotationMatchingPointcut.forMethodAnnotation(DynamicCacheable.class);
        final Advice advice = new DynamicCacheableInterceptor(cacheManager, dynamicCacheRegistry);
        return new DefaultPointcutAdvisor(pointcut, advice);
    }
}
