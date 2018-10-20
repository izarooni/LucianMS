package com.lucianms.server;

import com.lucianms.client.MapleCharacter;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author izarooni
 */
public final class PlayerStorage {

    private final ConcurrentHashMap<Integer, MapleCharacter> storage = new ConcurrentHashMap<>(200);

    public void clear() {
        storage.clear();
    }

    public void addPlayer(MapleCharacter player) {
        storage.put(player.getId(), player);
    }

    public MapleCharacter removePlayer(int playerID) {
        return storage.remove(playerID);
    }

    public MapleCharacter getPlayerByName(String name) {
        for (MapleCharacter player : storage.values()) {
            if (player.getName().toLowerCase().equals(name.toLowerCase())) {
                return player;
            }
        }
        return null;
    }

    public MapleCharacter getPlayerByID(int playerID) {
        return storage.get(playerID);
    }

    public ArrayList<MapleCharacter> getAllPlayers() {
        return new ArrayList<>(storage.values());
    }

    public int size() {
        return storage.size();
    }
}