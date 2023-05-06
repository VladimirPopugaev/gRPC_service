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
        if (maxSize < 1) {
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

    public V put(K key, V value) {
        if (key == null || value == null) {
            throw new NullPointerException("key and value cannot be null");
        }

        if (generalCache.containsKey(key)) {
                return generalCache.put(key, value);
        }

        V result;
        synchronized (this) {
            boolean hasFreeSlot = addToFreeSlot(key);

            if (hasFreeSlot) {
                generalCache.put(key, value);
                return value;
            } else {
                if (trimContainerIn()) {
                    generalCache.put(key, value);
                    addToContainerIn(key);
                    result = value;
                } else {
                    generalCache.put(key, value);
                    addToContainerHot(key);
                    trimContainerHot();
                    result = value;
                }
            }
        }

        return result;
    }

    /**
     * Deletes a value from the cache and all its containers.
     *
     * @param key key for deleting
     * @return removed value
     */
    public V remove(K key) {
        if (key == null) {
            throw new NullPointerException("key is cannot be null");
        }

        V removedValue;
        synchronized (this) {
            removedValue = generalCache.remove(key);
            if (removedValue != null) {
                removeFromAllContainers(key);
            }
        }

        return removedValue;
    }

    private void removeFromAllContainers(K key) {
        if (containerIn.contains(key)) {
            removeFromContainerIn(key);
        }

        if (containerOut.contains(key)) {
            removeFromContainerOut(key);
        }

        if (containerHot.contains(key)) {
            removeFromContainerHot(key);
        }
    }

    /**
     * If one of the containers has free space, it adds an item to it and returns true.
     * Otherwise it returns false.
     * @param key key for adding to container
     * @return true if key was added to container, else false
     */
    private boolean addToFreeSlot(K key) {
        boolean wasAddedToContainer = false;

        if ((maxSizeIn >= sizeIn + 1)) {
            addToContainerIn(key);
            wasAddedToContainer = true;
        }

        if (!wasAddedToContainer && (maxSizeOut >= sizeOut + 1)) {
            addToContainerOut(key);
            wasAddedToContainer = true;
        }

        if (!wasAddedToContainer && (maxSizeHot >= sizeHot + 1)) {
            addToContainerHot(key);
            wasAddedToContainer = true;
        }

        return wasAddedToContainer;
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

    /**
     * If there is space in the In container, one of its values is put on top.
     * If there is no space in the In container, one of its values is removed from
     * In container and moved to Out container.
     *
     * @return true if in `In` container have a space, else false
     */
    public boolean trimContainerIn() {
        boolean result = false;
        if (maxSizeIn < 1) {
            return false;
        }

        while (containerIn.iterator().hasNext()) {
            K keyIn = containerIn.iterator().next();

            if (isCorrectSizeContainerIn()) {
                result = true;
                break;
            } else {
                replaceFromInToOut(keyIn);
            }
        }
        return result;
    }

    private void replaceFromInToOut(K key) {
        removeFromContainerIn(key);

        while (containerOut.iterator().hasNext()) {
            if (isCorrectSizeContainerOut()) {
                addToContainerOut(key);
                break;
            } else {
                K keyOut = containerOut.iterator().next();
                removeFromContainerOut(keyOut);
            }
        }
    }

    // ================== Support functions =====================
    // ======================== add =============================
    private void addToContainerIn(K key) {
        containerIn.add(key);
        sizeIn++;
    }

    private void addToContainerOut(K key) {
        containerOut.add(key);
        sizeOut++;
    }

    private void addToContainerHot(K key) {
        containerHot.add(key);
        sizeHot++;
    }

    // ======================== remove ==========================
    private void removeFromContainerIn(K key) {
        containerIn.remove(key);
        sizeIn--;
    }

    private void removeFromContainerOut(K key) {
        containerOut.remove(key);
        sizeOut--;
    }

    private void removeFromContainerHot(K key) {
        containerHot.remove(key);
        sizeHot--;
    }

    // ======================== correct size ==========================
    private boolean isCorrectSizeContainerIn() {
        return (sizeIn + 1 <= maxSizeIn) || containerIn.isEmpty();
    }

    private boolean isCorrectSizeContainerOut() {
        return sizeOut + 1 <= maxSizeOut || containerOut.isEmpty();
    }

    private boolean isCorrectSizeContainerHot() {
        return sizeHot <= maxSizeHot || containerHot.isEmpty();
    }

    @Override
    public synchronized final String toString() {
        return new StringBuilder()
                .append("Container In: ").append(containerIn)
                .append("\nContainer Out: ").append(containerOut)
                .append("\nContainer Hot: ").append(containerHot)
                .toString();
    }
}
