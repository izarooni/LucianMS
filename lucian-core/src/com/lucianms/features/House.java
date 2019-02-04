package com.lucianms.features;

import com.lucianms.constants.ServerConstants;
import com.lucianms.server.FieldBuilder;
import com.lucianms.server.MaplePortal;
import com.lucianms.server.maps.MapleMap;

/**
 * @author izarooni
 * @author Lucas (lucasdieswagger)
 */

public class House {

    private final int ownerID;
    private int mapID;
    private String password;
    private long billDate;
    private MapleMap map = null;

    public House(int ownerID, int mapID, String password, long billDate) {
        this.ownerID = ownerID;
        this.mapID = mapID;
        this.password = password;
        this.billDate = billDate;
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

    public long getBillDate() {
        return billDate;
    }

    public void setBillDate(long billDate) {
        this.billDate = billDate;
    }

    public MapleMap getMap() {
        if (map == null) {
            map = new FieldBuilder(0, 1, mapID)
                    .loadFootholds()
                    .loadPortals()
                    .build();
            map.setForcedReturnMap(ServerConstants.HOME_MAP);
            map.setReturnMapId(ServerConstants.HOME_MAP);

            for (MaplePortal portal : map.getPortals()) {
                portal.setPortalStatus(false);
                portal.setScriptName(null);
            }
        }
        return map;
    }
}
