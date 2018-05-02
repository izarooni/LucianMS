package com.lucianms.io;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;

/**
 * @author izarooni
 */
public class Config {

    private static final Logger LOGGER = LoggerFactory.getLogger(Config.class);
    private final Properties properties;

    public Config(JSONObject object) {
        properties = new Properties();
        for (String key : object.keySet()) {
            properties.put(key, object.get(key));
        }
    }

    public void clearCaches() {
        iaCache = new HashMap<>(((int) (iaCache.size() / 0.75)) + 1);

        System.gc();
    }

    public String getString(String key) {
        return (String) properties.get(key);
    }

    public Long getNumber(String key) {
        return Long.parseLong(properties.getProperty(key));
    }

    public Boolean getBoolean(String key) {
        return Boolean.parseBoolean(properties.getProperty(key));
    }

    // int array cache
    private HashMap<String, int[]> iaCache = new HashMap<>();

    public int[] getIntArray(String key) {
        int[] array = iaCache.getOrDefault(key, null);
        if (array != null) {
            return array;
        }
        String string = getString(key);
        String[] split = string.split(",");
        array = new int[split.length];
        for (int i = 0; i < split.length; i++) {
            array[i] = Integer.parseInt(split[i]);
        }
        Arrays.sort(array);
        iaCache.put(key, array);
        return array;
    }
}
