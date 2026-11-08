package com.walter.lifelog.shared.store.adapter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("RedisKeyValueStore 테스트")
class RedisKeyValueStoreTest {
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private RedisKeyValueStore sut;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        sut = new RedisKeyValueStore(redisTemplate);
    }

    @Test
    @DisplayName("increment - 증가된 값을 그대로 반환한다")
    void increment_returnsIncrementedValue() {
        // given
        when(valueOperations.increment("post_1")).thenReturn(43L);

        // when / then
        assertThat(sut.increment("post_1")).isEqualTo(43L);
    }

    @Test
    @DisplayName("increment - Redis 가 null 을 반환하면 0 을 반환한다")
    void increment_nullResult_returnsZero() {
        // given
        when(valueOperations.increment(anyString())).thenReturn(null);

        // when / then
        assertThat(sut.increment("post_1")).isZero();
    }

    @Test
    @DisplayName("get - 저장된 값을 그대로 반환한다")
    void get_returnsStoredValue() {
        // given
        when(valueOperations.get("key")).thenReturn("value");

        // when / then
        assertThat(sut.get("key")).isEqualTo("value");
    }

    @Test
    @DisplayName("set - TTL 과 함께 위임한다")
    void set_delegatesWithTtl() {
        // when
        sut.set("key", "value", Duration.ofMinutes(5));

        // then
        verify(valueOperations).set("key", "value", Duration.ofMinutes(5));
    }

    @Test
    @DisplayName("delete - 키 삭제를 위임한다")
    void delete_delegates() {
        // when
        sut.delete("key");

        // then
        verify(redisTemplate).delete("key");
    }
}
