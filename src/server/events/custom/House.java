package server.events.custom;

import server.FieldBuilder;
import server.maps.MapleMap;

/**
 * @author izarooni
 * @author Lucas (lucasdieswagger)
 */

public class House {

    private final int ownerID;
    private int mapID;
    private String password;
    private MapleMap map;

    public House(int ownerID, int mapID, String password) {
        this.ownerID = ownerID;
        this.mapID = mapID;
        this.password = password;

        map = new FieldBuilder(0, 0, mapID)
                .loadFootholds()
                .loadPortals()
                .build();
    }

    public int getOwnerID() {
        return ownerID;
    }

    public int getMapID() {
        return mapID;
    }

    public void setMapID(int mapID) {
        this.mapID = mapID;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public MapleMap getMap() {
        return map;
    }
}
