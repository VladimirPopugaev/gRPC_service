package com.popugaevvn.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;


/**
 * !!!Attention!!!
 * I used reflexion, although normally you should only use open methods.
 * I did this to make sure my cache was working properly.
 */
public class TwoQCacheTest {

    /**
     * Returns the state of the container whose name is specified by the first parameter.
     * It is implemented with reflexion.
     *
     * @param containerName name of inner private field with container
     * @param <T>           type of set who returned
     * @return set of keys from cache in container
     */
    private <T> Set<T> getInfoAboutContainer(String containerName) {
        try {
            Field containerInInfo = cache.getClass().getDeclaredField(containerName);
            // Change private to public
            containerInInfo.setAccessible(true);

            return (Set<T>) containerInInfo.get(cache);
        } catch (Exception e) {
            throw new RuntimeException("Something went wrong");
        }
    }

    private int getSizeFieldFromCache(String fieldName) {
        try {
            Field size = cache.getClass().getDeclaredField(fieldName);
            // Change private to public
            size.setAccessible(true);

            return (int) size.get(cache);
        } catch (Exception e) {
            throw new RuntimeException("Something went wrong");
        }
    }

    TwoQCache<String, Integer> cache = new TwoQCache<>(10);

    @DisplayName("Put null in cache")
    @Test
    void errorPutKeyIN() {
        String key = null;
        Integer value = null;

        assertThrows(NullPointerException.class, () -> cache.put(key, value));
    }

    @DisplayName("Success put to IN container")
    @Test
    void successPutKeyIN() {
        Set<String> containerIn = getInfoAboutContainer("containerIn");
        assertTrue(containerIn.isEmpty());

        String key = "test";
        Integer value = 1;

        cache.put(key, value);
        containerIn = getInfoAboutContainer("containerIn");
        int currentSizeIn = getSizeFieldFromCache("sizeIn");

        assertFalse(containerIn.isEmpty());
        assertEquals(1, containerIn.size());
        assertEquals(key, Arrays.stream(containerIn.toArray()).findFirst().orElse(null));
        assertEquals(1, currentSizeIn);
    }

    @DisplayName("Success put already putting key")
    @Test
    void successPutExistingKey() {
        String key = "test";
        Integer value = 1;

        // first put
        cache.put(key, value);
        Set<String> containerIn = getInfoAboutContainer("containerIn");
        assertEquals(1, containerIn.size());
        // second put already exist key
        cache.put(key, value);
        containerIn = getInfoAboutContainer("containerIn");
        assertEquals(1, containerIn.size());
    }

    @DisplayName("Sucess put to IN container 3 elements. One of this should be in OUT")
    @Test
    void successPutKeyINAndOut() {
        int maxSizeOfContainerIn = getSizeFieldFromCache("maxSizeIn");

        // Filling the container In completely
        for (int i = 1; i <= maxSizeOfContainerIn; i++) {
            cache.put(Integer.toString(i), i);
        }

        // This item should go straight into out, since there is free space there
        cache.put("3", 3);
        Set<String> containerOut = getInfoAboutContainer("containerOut");
        assertEquals(1, containerOut.size());
        assertEquals("3", Arrays.stream(containerOut.toArray()).findFirst().orElse(null));
        assertEquals(3, cache.get("3"));
    }

    @DisplayName("Success put 10 elements to fill the containers completely")
    @Test
    void successPutKeyHot() {
        for (int i = 1; i <= 10; i++) {
            cache.put(Integer.toString(i), i);
        }

        Set<String> containerHot = getInfoAboutContainer("containerHot");
        assertFalse(containerHot.isEmpty());
    }

    @DisplayName("Sucess put 10 elements to cache. Add one more elements and check all containers")
    @Test
    void successPutManyElements() {
        for (int i = 1; i <= 10; i++) {
            cache.put(Integer.toString(i), i);
        }

        cache.put("11", 11);
        Set<String> containerIn = getInfoAboutContainer("containerIn");
        assertTrue(containerIn.contains("11"));

        // value '1' should go to out container
        Set<String> containerOut = getInfoAboutContainer("containerOut");
        assertTrue(containerOut.contains("1"));
    }


    @Test
    void successRemoveKey() throws NoSuchFieldException, IllegalAccessException {
        String key = "1";
        Integer value = 1;
        cache.put(key, value);

        cache.remove(key);
        Set<String> containerIn = getInfoAboutContainer("containerIn");
        Field mapInfo = cache.getClass().getDeclaredField("generalCache");
        // Change private to public
        mapInfo.setAccessible(true);
        HashMap<String, Integer> cacheMap = (HashMap<String, Integer>) mapInfo.get(cache);

        assertFalse(containerIn.contains(key));
        assertFalse(cacheMap.containsKey(key));
    }

    @Test
    void successGet() {
        String key = "1";
        Integer value = 12312;
        Integer addedElem = cache.put(key, value);

        Integer gettedElem = cache.get(key);

        assertEquals(addedElem, gettedElem);
    }

    @Test
    void testGetKeyException() {
        assertThrows(NullPointerException.class, () -> cache.get(null));
    }

}