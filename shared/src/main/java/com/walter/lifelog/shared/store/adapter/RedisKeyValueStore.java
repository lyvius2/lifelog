package com.walter.lifelog.shared.store.adapter;

import com.walter.lifelog.shared.store.KeyValueStore;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@Profile("live")
public class RedisKeyValueStore implements KeyValueStore {
    private final StringRedisTemplate redisTemplate;

    public RedisKeyValueStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public long increment(String key) {
        final Long result = redisTemplate.opsForValue().increment(key);
        return result != null ? result : 0L;
    }

    @Override
    @Nullable
    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public void set(String key, String value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    @Override
    public void delete(String key) {
        redisTemplate.delete(key);
    }
}
