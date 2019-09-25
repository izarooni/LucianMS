package com.lucianms;

import com.lucianms.io.defaults.Defaults;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashSet;

/**
 * @author izarooni
 */
public class Whitelist {

    private static final Logger LOGGER = LoggerFactory.getLogger(Whitelist.class);
    private static final String FileName = "whitelist.json";
    private static HashSet<Integer> accounts;

    public static boolean saveCache() {
        try (FileWriter w = new FileWriter(FileName)) {
            JSONObject o = new JSONObject();
            o.put("accounts", accounts);
            w.write(o.toString());
            w.flush();
        } catch (IOException e) {
            LOGGER.error("Failed to write cache", e);
        }
        return true;
    }

    public static int createCache() throws IOException, URISyntaxException {
        accounts = new HashSet<>();
        Defaults.createDefaultIfAbsent(null, FileName);
        JSONObject json = new JSONObject(new JSONTokener(new FileInputStream(FileName)));
        JSONArray jarr = json.getJSONArray("accounts");
        for (int i = 0; i < jarr.length(); i++) {
            int accountID = jarr.getInt(i);
            accounts.add(accountID);
        }
        return accounts.size();
    }

    public static HashSet<Integer> getAccounts() {
        return accounts;
    }

    public static boolean hasAccount(int accountID) {
        return accounts.contains(accountID);
    }
}
