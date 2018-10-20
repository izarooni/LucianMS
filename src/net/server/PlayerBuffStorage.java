package net.server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author izarooni
 */
public class PlayerBuffStorage {

    private ConcurrentHashMap<Integer, ArrayList<PlayerBuffValueHolder>> buffs = new ConcurrentHashMap<>(200);

    public void put(int playerID, ArrayList<PlayerBuffValueHolder> toStore) {
        buffs.put(playerID, toStore);
    }

    public ArrayList<PlayerBuffValueHolder> remove(int playerID) {
        return buffs.remove(playerID);
    }
}
