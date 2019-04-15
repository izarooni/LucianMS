package com.lucianms.server.world;

import com.lucianms.client.MapleCharacter;
import com.lucianms.server.maps.MapleDoor;

import java.util.ArrayList;

/**
 * @author izarooni
 */
public class MaplePartyCharacter extends SocialMember {

    //region for offline use
    private int level;
    private int jobID;
    private int fieldID;
    //endregion
    private ArrayList<MapleDoor> door = new ArrayList<>();

    public MaplePartyCharacter() {
        super(null);
        setChannelID(-2);
        // placeholder for packets, don't remove!
    }

    public MaplePartyCharacter(MapleCharacter player) {
        super(player);

        level = player.getLevel();
        jobID = player.getJob().getId();
        fieldID = player.getMapId();
        door.addAll(player.getDoors());
    }

    public int getLevel() {
        return level;
    }

    public int getJobID() {
        return jobID;
    }

    public int getFieldID() {
        return fieldID;
    }

    public void setFieldID(int fieldID) {
        this.fieldID = fieldID;
    }

    @Deprecated
    public void updateDoor(MapleDoor door) {
        this.door.add(door);
    }

    @Deprecated
    public ArrayList<MapleDoor> getDoors() {
        return door;
    }
}
