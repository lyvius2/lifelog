package com.walter.lifelog.shared.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("ViewCountHelper 테스트")
class ViewCountHelperTest {
    private ValueOperations<String, String> valueOperations;
    private ViewCountHelper sut;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        sut = new ViewCountHelper(redisTemplate);
    }

    @Test
    @DisplayName("increment - 증가된 값을 그대로 반환한다")
    void increment_returnsIncrementedValue() {
        // given
        when(valueOperations.increment("post_1")).thenReturn(43L);

        // when
        final long actual = sut.increment("post_1");

        // then
        assertThat(actual).isEqualTo(43L);
    }

    @Test
    @DisplayName("increment - Redis 가 null 을 반환하면 0 을 반환한다")
    void increment_nullResult_returnsZero() {
        // given
        when(valueOperations.increment(anyString())).thenReturn(null);

        // when
        final long actual = sut.increment("post_1");

        // then
        assertThat(actual).isZero();
    }

    @Test
    @DisplayName("getCount - 저장된 숫자 문자열을 Integer 로 반환한다")
    void getCount_numericValue_returnsInteger() {
        // given
        when(valueOperations.get("post_1")).thenReturn("150");

        // when
        final Integer actual = sut.getCount("post_1");

        // then
        assertThat(actual).isEqualTo(150);
    }

    @Test
    @DisplayName("getCount - 키가 없으면 null 을 반환한다")
    void getCount_missingKey_returnsNull() {
        // given
        when(valueOperations.get("post_2")).thenReturn(null);

        // when
        final Integer actual = sut.getCount("post_2");

        // then
        assertThat(actual).isNull();
    }

    @Test
    @DisplayName("getCount - 값이 숫자가 아니면 null 을 반환한다")
    void getCount_nonNumericValue_returnsNull() {
        // given
        when(valueOperations.get("post_3")).thenReturn("not_a_number");

        // when
        final Integer actual = sut.getCount("post_3");

        // then
        assertThat(actual).isNull();
    }
}
