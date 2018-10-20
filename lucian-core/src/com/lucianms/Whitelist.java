package com.lucianms;

import com.lucianms.io.defaults.Defaults;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author izarooni
 */
public class Whitelist {

    private static List<Integer> accounts = null;

    public static void loadAccounts() throws IOException, URISyntaxException {
        if (accounts != null) {
            accounts.clear();
        }
        accounts = new ArrayList<>();
        Defaults.createDefaultIfAbsent(null, "whitelist.json");
        JSONObject json = new JSONObject(new JSONTokener(new FileInputStream("whitelist.json")));
        String aString = json.getString("accounts");
        for (String split : aString.split(", ")) {
            try {
                int accountID = Integer.parseInt(split);
                accounts.add(accountID);
            } catch (NumberFormatException e) {
                System.err.println(String.format("Unable to parse account id '%s' for whitelisting", split));
            }
        }
    }

    public static List<Integer> getAccounts() {
        return new ArrayList<>(accounts);
    }

    public static boolean hasAccount(int accountID) {
        return accounts.indexOf(accountID) > -1;
    }
}
