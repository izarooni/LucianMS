package com.lucianms.lang;

import java.util.Hashtable;

/**
 * @author izarooni
 */
public class GProperties<V> extends Hashtable<String, V> {

    public GProperties() {
        super(10);
    }

    public boolean checkProperty(String key, Object value) {
        return get(key) != null && get(key).equals(value);
    }
}
