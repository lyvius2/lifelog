package com.walter.lifelog.shared.store.adapter;

import com.walter.lifelog.shared.store.KeyValueStore;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.LongSupplier;

/**
 * Redis 없이 도는 로컬 메모리 어댑터. live 가 아닌 프로파일에서만 쓴다.
 *
 * <p>만료는 조회 시점에 판정하고(lazy), 항목 수가 {@link #MAX_ENTRIES} 를 넘을 때만
 * 만료된 항목을 쓸어낸다. 프로세스가 끝나면 사라지고 인스턴스 간에 공유되지 않는다.
 */
@Component
@Profile("!live")
public class InMemoryKeyValueStore implements KeyValueStore {
    private static final int MAX_ENTRIES = 10_000;

    private final ConcurrentMap<String, Entry> store = new ConcurrentHashMap<>();
    private final LongSupplier ticker;

    public InMemoryKeyValueStore() {
        this(System::nanoTime);
    }

    InMemoryKeyValueStore(LongSupplier ticker) {
        this.ticker = ticker;
    }

    @Override
    public long increment(String key) {
        final Entry updated = store.compute(key, (ignored, current) -> {
            final long base = (current == null || current.isExpired(ticker.getAsLong())) ? 0L : current.asLong();
            return Entry.withoutExpiry(Long.toString(base + 1));
        });
        return updated.asLong();
    }

    @Override
    @Nullable
    public String get(String key) {
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
    public void set(String key, String value, Duration ttl) {
        purgeExpiredIfCrowded();
        store.put(key, Entry.expiringAt(value, ticker.getAsLong() + ttl.toNanos()));
    }

    @Override
    public void delete(String key) {
        store.remove(key);
    }

    private void purgeExpiredIfCrowded() {
        if (store.size() < MAX_ENTRIES) {
            return;
        }
        final long now = ticker.getAsLong();
        store.values().removeIf(entry -> entry.isExpired(now));
    }

    private record Entry(String value, boolean hasExpiry, long expiresAt) {

        static Entry withoutExpiry(String value) {
            return new Entry(value, false, 0L);
        }

        static Entry expiringAt(String value, long expiresAt) {
            return new Entry(value, true, expiresAt);
        }

        boolean isExpired(long now) {
            return hasExpiry && now - expiresAt >= 0;
        }

        /** 숫자가 아닌 값은 0 으로 본다. Redis 의 INCR 은 오류지만 조회수 집계를 멈출 이유는 없다. */
        long asLong() {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                return 0L;
            }
        }
    }
}
