package com.lucianms.features.carnival;

import com.lucianms.server.channel.MapleChannel;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author izarooni
 */
public class MCarnivalLobbyManager {

    private LinkedHashMap<Integer, MCarnivalLobby> lobbies = new LinkedHashMap<>();

    public MCarnivalLobbyManager(MapleChannel channel) {
        final int bMapId = 980000000;
        for (int i = 1; i < 7; i++) {
            int nMapId = bMapId + (i * 100);
            int maxPartySize;
            if (i == 1 || i == 2) {
                maxPartySize = 2;
            } else if (i == 3 || i == 4) {
                maxPartySize = 3;
            } else {
                maxPartySize = 5;
            }
            lobbies.put(nMapId, new MCarnivalLobby(channel, maxPartySize, nMapId));
        }
    }

    public void reset() {
        for (MCarnivalLobby lobby : lobbies.values()) {
            lobby.setState(MCarnivalLobby.State.Available);
        }
    }

    public MCarnivalLobby getLobby(int mapId) {
        return lobbies.get(mapId);
    }

    public Map<Integer, MCarnivalLobby> getLobbies() {
        return Collections.unmodifiableMap(lobbies);
    }
}
