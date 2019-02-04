package com.lucianms.lang;

import java.util.Hashtable;

/**
 * @author izarooni
 */
public class GProperties<V> extends Hashtable<String, V> {

    public GProperties() {
        super(10);
    }

    /**
     * Verify the existence of a specified property.
     * <p>
     * Set the value should it not exist
     * </p>
     *
     * @param key   the property key
     * @param value the property value
     * @return the existing value or the specified one if one doesn't exist
     */
    public V checkProperty(String key, V value) {
        return computeIfAbsent(key, k -> value);
    }
}
