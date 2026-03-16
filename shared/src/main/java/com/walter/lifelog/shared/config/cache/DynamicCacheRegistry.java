package com.walter.lifelog.shared.config.cache;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class DynamicCacheRegistry {
    private static final ConcurrentMap<String, Duration> TTL_MAP = new ConcurrentHashMap<>();

    public void register(String cacheName, long ttlMinutes) {
        TTL_MAP.putIfAbsent(cacheName, Duration.ofMinutes(ttlMinutes));
    }

    public Duration getTtl(String cacheName) {
        return TTL_MAP.get(cacheName);
    }
}
