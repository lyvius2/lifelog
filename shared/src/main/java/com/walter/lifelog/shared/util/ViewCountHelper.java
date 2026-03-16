package com.walter.lifelog.shared.util;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class ViewCountHelper {
    private final StringRedisTemplate redisTemplate;

    public ViewCountHelper(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public long increment(String key) {
        final Long result = redisTemplate.opsForValue().increment(key);
        return result != null ? result : 0L;
    }
}