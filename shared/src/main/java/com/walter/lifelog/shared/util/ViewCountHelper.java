package com.walter.lifelog.shared.util;

import com.walter.lifelog.shared.store.KeyValueStore;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

@Component
public class ViewCountHelper {
    private final KeyValueStore keyValueStore;

    public ViewCountHelper(KeyValueStore keyValueStore) {
        this.keyValueStore = keyValueStore;
    }

    public long increment(String key) {
        return keyValueStore.increment(key);
    }

    /**
     * 저장된 카운트를 반환합니다. 키가 없거나 값이 숫자가 아니면 {@code null} 입니다.
     */
    @Nullable
    public Integer getCount(String key) {
        final String value = keyValueStore.get(key);
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
