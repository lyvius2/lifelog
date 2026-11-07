package com.walter.lifelog.shared.util;

import org.jetbrains.annotations.Nullable;
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

    /**
     * 저장된 카운트를 반환합니다. 키가 없거나 값이 숫자가 아니면 {@code null} 입니다.
     */
    @Nullable
    public Integer getCount(String key) {
        final String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}