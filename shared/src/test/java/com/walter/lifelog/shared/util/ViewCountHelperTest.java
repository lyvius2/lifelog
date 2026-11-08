package com.walter.lifelog.shared.util;

import com.walter.lifelog.shared.store.KeyValueStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("ViewCountHelper 테스트")
class ViewCountHelperTest {
    private KeyValueStore keyValueStore;
    private ViewCountHelper sut;

    @BeforeEach
    void setUp() {
        keyValueStore = mock(KeyValueStore.class);
        sut = new ViewCountHelper(keyValueStore);
    }

    @Test
    @DisplayName("increment - 증가된 값을 그대로 반환한다")
    void increment_returnsIncrementedValue() {
        // given
        when(keyValueStore.increment("post_1")).thenReturn(43L);

        // when
        final long actual = sut.increment("post_1");

        // then
        assertThat(actual).isEqualTo(43L);
    }

    @Test
    @DisplayName("increment - 저장소가 0 을 반환하면 0 을 반환한다")
    void increment_zeroResult_returnsZero() {
        // given
        when(keyValueStore.increment(anyString())).thenReturn(0L);

        // when
        final long actual = sut.increment("post_1");

        // then
        assertThat(actual).isZero();
    }

    @Test
    @DisplayName("getCount - 저장된 숫자 문자열을 Integer 로 반환한다")
    void getCount_numericValue_returnsInteger() {
        // given
        when(keyValueStore.get("post_1")).thenReturn("150");

        // when
        final Integer actual = sut.getCount("post_1");

        // then
        assertThat(actual).isEqualTo(150);
    }

    @Test
    @DisplayName("getCount - 키가 없으면 null 을 반환한다")
    void getCount_missingKey_returnsNull() {
        // given
        when(keyValueStore.get("post_2")).thenReturn(null);

        // when
        final Integer actual = sut.getCount("post_2");

        // then
        assertThat(actual).isNull();
    }

    @Test
    @DisplayName("getCount - 값이 숫자가 아니면 null 을 반환한다")
    void getCount_nonNumericValue_returnsNull() {
        // given
        when(keyValueStore.get("post_3")).thenReturn("not_a_number");

        // when
        final Integer actual = sut.getCount("post_3");

        // then
        assertThat(actual).isNull();
    }
}
