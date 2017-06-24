package server.partyquest.carnival;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author izarooni
 */
public class MCarnivalLobbyManager {

    private LinkedHashMap<Integer, MCarnivalLobby> lobbies = new LinkedHashMap<>();

    public MCarnivalLobbyManager() {
        int bMapId = 980000000;
        for (int i = 1; i < 7; i++) {
            int nMapId = bMapId + (i * 100);
            lobbies.put(nMapId, new MCarnivalLobby(nMapId + 1));
        }
    }

    public MCarnivalLobby getLobby(int mapId) {
        return lobbies.get(mapId);
    }

    public Map<Integer, MCarnivalLobby> getLobbies() {
        return Collections.unmodifiableMap(lobbies);
    }
}
