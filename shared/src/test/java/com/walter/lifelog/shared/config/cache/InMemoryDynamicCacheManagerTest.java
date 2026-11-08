package com.walter.lifelog.shared.config.cache;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link DynamicCacheRegistry} 의 TTL 맵이 static 이라 캐시 이름이 테스트 사이에 남습니다.
 * 그래서 이름마다 다른 값을 쓰고, 실제 애플리케이션의 캐시 이름과 겹치지 않게 합니다.
 */
@DisplayName("InMemoryDynamicCacheManager 테스트")
class InMemoryDynamicCacheManagerTest {
    private static final Duration DEFAULT_TTL = Duration.ofHours(24);

    private final AtomicLong now = new AtomicLong(0L);
    private final DynamicCacheRegistry registry = new DynamicCacheRegistry();
    private final InMemoryDynamicCacheManager sut =
            new InMemoryDynamicCacheManager(registry, DEFAULT_TTL, now::get);

    @Test
    @DisplayName("getCache - 같은 이름은 같은 인스턴스를 돌려준다")
    void getCache_sameName_returnsSameInstance() {
        // when / then
        assertThat(sut.getCache("test-same")).isSameAs(sut.getCache("test-same"));
    }

    @Test
    @DisplayName("put / get - 저장한 값을 돌려준다")
    void put_thenGet_returnsValue() {
        // given
        final Cache cache = sut.getCache("test-put");

        // when
        cache.put("key", "value");

        // then
        assertThat(cache.get("key")).isNotNull();
        assertThat(cache.get("key").get()).isEqualTo("value");
    }

    @Test
    @DisplayName("get - 저장하지 않은 키는 null 이다")
    void get_missingKey_returnsNull() {
        // when / then
        assertThat(sut.getCache("test-missing").get("absent")).isNull();
    }

    @Test
    @DisplayName("TTL - 등록된 TTL 이 지나면 만료된다")
    void entry_expiresAfterRegisteredTtl() {
        // given
        registry.register("test-ttl-expired", 10);
        final Cache cache = sut.getCache("test-ttl-expired");
        cache.put("key", "value");

        // when
        now.addAndGet(Duration.ofMinutes(10).toNanos());

        // then
        assertThat(cache.get("key")).isNull();
    }

    @Test
    @DisplayName("TTL - 등록된 TTL 이 지나기 전에는 유지된다")
    void entry_survivesBeforeRegisteredTtl() {
        // given
        registry.register("test-ttl-alive", 10);
        final Cache cache = sut.getCache("test-ttl-alive");
        cache.put("key", "value");

        // when
        now.addAndGet(Duration.ofMinutes(10).toNanos() - 1);

        // then
        assertThat(cache.get("key")).isNotNull();
    }

    @Test
    @DisplayName("TTL - 등록되지 않은 캐시는 기본 TTL 을 쓴다")
    void unregisteredCache_usesDefaultTtl() {
        // given
        final Cache cache = sut.getCache("test-default-ttl");
        cache.put("key", "value");

        // when
        now.addAndGet(DEFAULT_TTL.toNanos() - 1);

        // then
        assertThat(cache.get("key")).isNotNull();

        // when
        now.addAndGet(1);

        // then
        assertThat(cache.get("key")).isNull();
    }

    @Test
    @DisplayName("put - null 값은 저장하지 않는다")
    void put_nullValue_isNotCached() {
        // given
        final Cache cache = sut.getCache("test-null");

        // when
        cache.put("key", null);

        // then
        assertThat(cache.get("key")).isNull();
    }

    @Test
    @DisplayName("evict - 지정한 키만 지운다")
    void evict_removesOnlyGivenKey() {
        // given
        final Cache cache = sut.getCache("test-evict");
        cache.put("a", "1");
        cache.put("b", "2");

        // when
        cache.evict("a");

        // then
        assertThat(cache.get("a")).isNull();
        assertThat(cache.get("b")).isNotNull();
    }

    @Test
    @DisplayName("clear - 전부 지운다")
    void clear_removesAll() {
        // given
        final Cache cache = sut.getCache("test-clear");
        cache.put("a", "1");
        cache.put("b", "2");

        // when
        cache.clear();

        // then
        assertThat(cache.get("a")).isNull();
        assertThat(cache.get("b")).isNull();
    }

    @Test
    @DisplayName("get(key, loader) - 값이 없으면 loader 결과를 저장하고 돌려준다")
    void getWithLoader_missingKey_loadsAndCaches() {
        // given
        final Cache cache = sut.getCache("test-loader");

        // when
        final String loaded = cache.get("key", () -> "loaded");

        // then
        assertThat(loaded).isEqualTo("loaded");
        assertThat(cache.get("key").get()).isEqualTo("loaded");
    }

    @Test
    @DisplayName("get(key, loader) - 값이 있으면 loader 를 호출하지 않는다")
    void getWithLoader_existingKey_doesNotLoad() {
        // given
        final Cache cache = sut.getCache("test-loader-hit");
        cache.put("key", "cached");

        // when
        final String actual = cache.get("key", () -> {
            throw new IllegalStateException("loader 가 호출되면 안 됩니다");
        });

        // then
        assertThat(actual).isEqualTo("cached");
    }

    @Test
    @DisplayName("getCacheNames - 생성된 캐시 이름을 돌려준다")
    void getCacheNames_returnsCreatedCaches() {
        // given
        sut.getCache("test-name-a");
        sut.getCache("test-name-b");

        // when / then
        assertThat(sut.getCacheNames()).containsExactlyInAnyOrder("test-name-a", "test-name-b");
    }
}
