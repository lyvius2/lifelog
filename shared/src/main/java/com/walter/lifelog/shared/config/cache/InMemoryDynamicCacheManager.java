package com.walter.lifelog.shared.config.cache;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.AbstractValueAdaptingCache;

import java.time.Duration;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.LongSupplier;

/**
 * {@link DynamicRedisCacheManager} 의 로컬 메모리 짝. live 가 아닌 프로파일에서만 쓴다.
 *
 * <p>TTL 은 Redis 쪽과 같은 {@link DynamicCacheRegistry} 에서 읽으므로
 * {@code @DynamicCacheable(ttlMinutes = ...)} 의 의미가 프로파일에 따라 달라지지 않는다.
 * null 값은 Redis 설정의 {@code disableCachingNullValues()} 와 맞춰 저장하지 않는다.
 */
public class InMemoryDynamicCacheManager implements CacheManager {
    private final DynamicCacheRegistry dynamicCacheRegistry;
    private final Duration defaultTtl;
    private final LongSupplier ticker;
    private final ConcurrentMap<String, Cache> caches = new ConcurrentHashMap<>();

    public InMemoryDynamicCacheManager(DynamicCacheRegistry dynamicCacheRegistry, Duration defaultTtl) {
        this(dynamicCacheRegistry, defaultTtl, System::nanoTime);
    }

    InMemoryDynamicCacheManager(DynamicCacheRegistry dynamicCacheRegistry, Duration defaultTtl, LongSupplier ticker) {
        this.dynamicCacheRegistry = dynamicCacheRegistry;
        this.defaultTtl = defaultTtl;
        this.ticker = ticker;
    }

    @Override
    public Cache getCache(@NotNull String name) {
        return caches.computeIfAbsent(name, cacheName -> new TtlCache(cacheName, ttlOf(cacheName), ticker));
    }

    @Override
    @NotNull
    public Collection<String> getCacheNames() {
        return Set.copyOf(caches.keySet());
    }

    private Duration ttlOf(String cacheName) {
        final Duration registered = dynamicCacheRegistry.getTtl(cacheName);
        return registered != null ? registered : defaultTtl;
    }

    private static final class TtlCache extends AbstractValueAdaptingCache {
        private final String name;
        private final Duration ttl;
        private final LongSupplier ticker;
        private final ConcurrentMap<Object, Entry> store = new ConcurrentHashMap<>();

        private TtlCache(String name, Duration ttl, LongSupplier ticker) {
            super(false);
            this.name = name;
            this.ttl = ttl;
            this.ticker = ticker;
        }

        @Override
        @NotNull
        public String getName() {
            return name;
        }

        @Override
        @NotNull
        public Object getNativeCache() {
            return store;
        }

        @Override
        @Nullable
        protected Object lookup(@NotNull Object key) {
            final Entry entry = store.get(key);
            if (entry == null) {
                return null;
            }
            if (entry.isExpired(ticker.getAsLong())) {
                store.remove(key, entry);
                return null;
            }
            return entry.value();
        }

        @Override
        public void put(@NotNull Object key, @Nullable Object value) {
            if (value == null) {
                store.remove(key);
                return;
            }
            store.put(key, new Entry(value, ticker.getAsLong() + ttl.toNanos()));
        }

        @Override
        public void evict(@NotNull Object key) {
            store.remove(key);
        }

        @Override
        public void clear() {
            store.clear();
        }

        @Override
        @Nullable
        public <T> T get(@NotNull Object key, @NotNull Callable<T> valueLoader) {
            final ValueWrapper cached = get(key);
            if (cached != null) {
                @SuppressWarnings("unchecked")
                final T existing = (T) cached.get();
                return existing;
            }
            final T loaded;
            try {
                loaded = valueLoader.call();
            } catch (Exception e) {
                throw new ValueRetrievalException(key, valueLoader, e);
            }
            put(key, loaded);
            return loaded;
        }

        private record Entry(Object value, long expiresAt) {
            boolean isExpired(long now) {
                return now - expiresAt >= 0;
            }
        }
    }
}
