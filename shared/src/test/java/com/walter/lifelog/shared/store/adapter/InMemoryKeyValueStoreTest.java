package com.walter.lifelog.shared.store.adapter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InMemoryKeyValueStore 테스트")
class InMemoryKeyValueStoreTest {
    private final AtomicLong now = new AtomicLong(0L);
    private final InMemoryKeyValueStore sut = new InMemoryKeyValueStore(now::get);

    @Test
    @DisplayName("increment - 없는 키는 1 부터 시작해 호출마다 1 씩 올라간다")
    void increment_missingKey_startsAtOne() {
        // when / then
        assertThat(sut.increment("post_1")).isEqualTo(1L);
        assertThat(sut.increment("post_1")).isEqualTo(2L);
        assertThat(sut.increment("post_1")).isEqualTo(3L);
    }

    @Test
    @DisplayName("increment - 키마다 따로 센다")
    void increment_countsPerKey() {
        // given
        sut.increment("post_1");
        sut.increment("post_1");

        // when / then
        assertThat(sut.increment("post_2")).isEqualTo(1L);
        assertThat(sut.get("post_1")).isEqualTo("2");
    }

    @Test
    @DisplayName("increment - TTL 을 설정하지 않으므로 시간이 지나도 유지된다")
    void increment_hasNoExpiry() {
        // given
        sut.increment("post_1");

        // when
        now.addAndGet(Duration.ofDays(365).toNanos());

        // then
        assertThat(sut.get("post_1")).isEqualTo("1");
    }

    @Test
    @DisplayName("get - 없는 키는 null 이다")
    void get_missingKey_returnsNull() {
        // when / then
        assertThat(sut.get("absent")).isNull();
    }

    @Test
    @DisplayName("set / get - TTL 이 지나기 전에는 값을 돌려준다")
    void set_beforeTtl_returnsValue() {
        // given
        sut.set("token", "value", Duration.ofMinutes(5));

        // when
        now.addAndGet(Duration.ofMinutes(5).toNanos() - 1);

        // then
        assertThat(sut.get("token")).isEqualTo("value");
    }

    @Test
    @DisplayName("set / get - TTL 이 지나면 null 이다")
    void set_afterTtl_returnsNull() {
        // given
        sut.set("token", "value", Duration.ofMinutes(5));

        // when
        now.addAndGet(Duration.ofMinutes(5).toNanos());

        // then
        assertThat(sut.get("token")).isNull();
    }

    @Test
    @DisplayName("set - 같은 키를 다시 쓰면 값과 TTL 이 모두 갱신된다")
    void set_sameKey_replacesValueAndTtl() {
        // given
        sut.set("token", "old", Duration.ofMinutes(1));
        now.addAndGet(Duration.ofSeconds(30).toNanos());

        // when
        sut.set("token", "new", Duration.ofMinutes(10));
        now.addAndGet(Duration.ofMinutes(5).toNanos());

        // then
        assertThat(sut.get("token")).isEqualTo("new");
    }

    @Test
    @DisplayName("delete - 키를 지운다")
    void delete_removesKey() {
        // given
        sut.set("token", "value", Duration.ofMinutes(5));

        // when
        sut.delete("token");

        // then
        assertThat(sut.get("token")).isNull();
    }

    @Test
    @DisplayName("delete - 없는 키를 지워도 예외가 없다")
    void delete_missingKey_doesNotThrow() {
        // when / then
        sut.delete("absent");
        assertThat(sut.get("absent")).isNull();
    }

    @Test
    @DisplayName("increment - 만료된 값 위에서는 1 부터 다시 센다")
    void increment_afterExpiredValue_restartsFromOne() {
        // given
        sut.set("post_1", "99", Duration.ofMinutes(1));

        // when
        now.addAndGet(Duration.ofMinutes(2).toNanos());

        // then
        assertThat(sut.increment("post_1")).isEqualTo(1L);
    }

    @Test
    @DisplayName("increment - 숫자가 아닌 값 위에서는 1 로 센다")
    void increment_onNonNumericValue_countsFromOne() {
        // given
        sut.set("post_1", "not_a_number", Duration.ofMinutes(10));

        // when / then
        assertThat(sut.increment("post_1")).isEqualTo(1L);
    }
}
