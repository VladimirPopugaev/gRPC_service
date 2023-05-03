package com.popugaevvn.repository;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

// TODO: добавить интерфейс, чтоб можно было изменить реализацию в какой-то момент
public class TwoQCache<K, V> {

    /**
     * Primary in memory repository
     */
    final HashMap<K, V> generalCache;

    /**
     * Containers where keys are stored for quick access
     */
    private final Set<K> containerIn; // 1-st level: store for input keys, FIFO
    private final Set<K> containerOut; // 2-nd level: store for keys goes from input to output, FIFO
    private final Set<K> containerHot; // store for keys goes from output to hot, LRU

    /**
     * Will store the current number of items in the container
     */
    private int sizeIn;
    private int sizeOut;
    private int sizeHot;

    /**
     * Will store the maximum size of containers
     */
    private int maxSizeIn;
    private int maxSizeOut;
    private int maxSizeHot;



    public TwoQCache(int maxSize) {
        if (maxSize < 0) {
            throw new IllegalArgumentException("size cannot be smaller than 1");
        }

        initContainersMaxSize(maxSize);

        this.generalCache = new HashMap<>(0);
        this.containerIn = new LinkedHashSet<>();
        this.containerOut = new LinkedHashSet<>();
        this.containerHot = new LinkedHashSet<>();
    }

    private void initContainersMaxSize(int maxSize) {
        this.maxSizeIn = Math.round(maxSize * 0.2f); // 20%
        this.maxSizeOut = Math.round(maxSize * 0.6f); // 60%
        this.maxSizeHot = Math.round(maxSize * 0.2f); // 20%
    }

    // TODO: добавить метод get. Такой же как в LRU
    // 1. Берём ключ. Если hot содержит его, то кладём наверх очереди
    // 2. Если не содержит, то проверяем в out. Если там есть, то добавляем в hot.
    // 2.1. Надо проверить, что не привысили размер. Отдельная функция

    // TODO: добавить метод проверки на достижение максимального размера
}
