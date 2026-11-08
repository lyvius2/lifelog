package com.walter.lifelog.shared.store;

import org.jetbrains.annotations.Nullable;

import java.time.Duration;

/**
 * 짧은 수명의 키·값을 저장하는 포트. live 는 Redis 어댑터, 그 외 프로파일은 로컬 메모리
 * 어댑터가 주입된다. 구현체는 Redis 의 동작에 맞춘다 — {@code increment} 는 TTL 을
 * 설정하지 않고, {@code get} 은 키가 없으면 {@code null} 을 돌려준다.
 */
public interface KeyValueStore {

    long increment(String key);

    @Nullable
    String get(String key);

    void set(String key, String value, Duration ttl);

    void delete(String key);
}
