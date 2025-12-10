package org.springframework.cglib.core.internal;

public interface Function<K, V> {
    V apply(K key);
}
