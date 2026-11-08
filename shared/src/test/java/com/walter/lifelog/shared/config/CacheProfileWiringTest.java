package com.walter.lifelog.shared.config;

import com.walter.lifelog.shared.config.cache.DynamicCacheRegistry;
import com.walter.lifelog.shared.config.cache.DynamicRedisCacheManager;
import com.walter.lifelog.shared.config.cache.InMemoryDynamicCacheManager;
import com.walter.lifelog.shared.store.KeyValueStore;
import com.walter.lifelog.shared.store.adapter.InMemoryKeyValueStore;
import com.walter.lifelog.shared.store.adapter.RedisKeyValueStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 프로파일에 따라 Redis 어댑터와 로컬 메모리 어댑터 중 하나만 뜨는지 고정합니다.
 */
@DisplayName("캐시·저장소 프로파일 배선 테스트")
class CacheProfileWiringTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            // RedisCacheConfig 의 @Value("${...}") 해석과 String -> Duration 변환에
            // 필요합니다. SpringApplication 이 평소 깔아 주는 것들입니다.
            .withBean(PropertySourcesPlaceholderConfigurer.class)
            .withBean("conversionService", ConversionService.class,
                    ApplicationConversionService::getSharedInstance)
            .withBean(DynamicCacheRegistry.class)
            .withBean(RedisConnectionFactory.class, () -> mock(RedisConnectionFactory.class))
            .withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class))
            .withUserConfiguration(RedisCacheConfig.class, InMemoryCacheConfig.class,
                    RedisKeyValueStore.class, InMemoryKeyValueStore.class);

    @Test
    @DisplayName("live - Redis 캐시 매니저와 Redis 저장소가 뜬다")
    void liveProfile_usesRedis() {
        runner.withPropertyValues("spring.profiles.active=live").run(context -> {
            assertThat(context.getBean(CacheManager.class)).isInstanceOf(DynamicRedisCacheManager.class);
            assertThat(context.getBean(KeyValueStore.class)).isInstanceOf(RedisKeyValueStore.class);
            assertThat(context).doesNotHaveBean(InMemoryKeyValueStore.class);
        });
    }

    @Test
    @DisplayName("default - 로컬 메모리 캐시 매니저와 로컬 메모리 저장소가 뜬다")
    void defaultProfile_usesInMemory() {
        runner.run(context -> {
            assertThat(context.getBean(CacheManager.class)).isInstanceOf(InMemoryDynamicCacheManager.class);
            assertThat(context.getBean(KeyValueStore.class)).isInstanceOf(InMemoryKeyValueStore.class);
            assertThat(context).doesNotHaveBean(RedisKeyValueStore.class);
        });
    }

    @Test
    @DisplayName("dev - 로컬 메모리 쪽이 뜬다")
    void devProfile_usesInMemory() {
        runner.withPropertyValues("spring.profiles.active=dev").run(context -> {
            assertThat(context.getBean(CacheManager.class)).isInstanceOf(InMemoryDynamicCacheManager.class);
            assertThat(context.getBean(KeyValueStore.class)).isInstanceOf(InMemoryKeyValueStore.class);
        });
    }
}
