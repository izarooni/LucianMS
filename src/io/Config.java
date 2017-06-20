package io;

import org.json.JSONObject;

import java.util.Properties;

/**
 * @author izarooni
 */
public class Config {

    private final Properties properties;

    public Config(JSONObject object) {
        properties = new Properties();
        for (String key : object.keySet()) {
            properties.put(key, object.get(key));
        }
    }

    public String getString(String key) {
        return (String) properties.get(key);
    }

    public Long getNumber(String key) { return Long.parseLong(properties.getProperty(key)); }
}
