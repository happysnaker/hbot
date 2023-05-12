package com.github.happysnaker.hbotcore.utils;

import java.util.List;
import java.util.Objects;

/**
 * @author Happysnaker
 * @description
 * @date 2022/1/30
 * @email happysnaker@foxmail.com
 */
public class Pair<K, V> {
    K key;
    V value;

    public static<K, V> Pair<K, V> of(K key, V val) {
        return new Pair<>(key, val);
    }

    public static<K, V> List<Pair<K, V>> ofList(K key1, V val1, K key2, V val2, K key3, V val3) {
        return List.of(Pair.of(key1, val1), Pair.of(key2, val2), Pair.of(key3, val3));
    }

    public void setKey(K key) {
        this.key = key;
    }

    public void setValue(V value) {
        this.value = value;
    }

    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pair<?, ?> pair = (Pair<?, ?>) o;
        return Objects.equals(key, pair.key) && Objects.equals(value, pair.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "Pair{" +
                "key=" + key +
                ", value=" + value +
                '}';
    }
}
