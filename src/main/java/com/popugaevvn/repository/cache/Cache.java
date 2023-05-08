package com.popugaevvn.repository.cache;

public interface Cache<K, V> {

    V put(K ket, V value);

    V get(K key);

    V remove(K key);
}
