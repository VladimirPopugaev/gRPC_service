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

    /**
     * Returns the value on the key. If the key is found, the container is changed.
     * If the key is not found, null is returned.
     *
     * @param key key by which the value is selected
     * @return Value by key or null
     * @throws NullPointerException if key is null
     */
    public V get(K key) {
        if (key == null) {
            throw new NullPointerException("key cannot is null");
        }

        synchronized (this) {
            V mapValue =  generalCache.get(key);
            if (mapValue != null) {
                replaceContainersPositions(key);
            }
            return mapValue;
        }
    }

    private void replaceContainersPositions(K key) {
        if (containerHot.contains(key)) {
            replaceContainerHotHead(key);
        } else {
            if (containerOut.contains(key)) {
                addToContainerHot(key);
                trimContainerHot();
                removeFromContainerOut(key);
            }
        }
    }

    private void replaceContainerHotHead(K key) {
        containerHot.remove(key);
        containerHot.add(key);
    }

    private void addToContainerHot(K key) {
        containerHot.add(key);
        sizeHot++;
    }

    private void removeFromContainerHot(K key) {
        containerHot.remove(key);
        sizeHot--;
    }

    private void removeFromContainerOut(K key) {
        sizeOut--;
        containerOut.remove(key);
    }

    /**
     * As long as the container size is not smaller than the maximum, removes items from the cache.
     */
    public void trimContainerHot() {
        while (true) {
            K key;

            synchronized (this) {
                if (sizeOut < 0 || (containerOut.isEmpty() && sizeHot != 0)) {
                    throw new IllegalStateException(getClass().getName() + ".sizeOf() is contains inconsistent results");
                }

                if (isCorrectSizeContainerHot()) {
                    break;
                }

                key = containerHot.iterator().next();
                removeFromContainerHot(key);

                generalCache.remove(key);
            }
        }
    }

    private boolean isCorrectSizeContainerHot() {
        return sizeHot <= maxSizeHot || containerHot.isEmpty();
    }


}
