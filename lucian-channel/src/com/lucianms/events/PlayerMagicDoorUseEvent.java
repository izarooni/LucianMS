package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.maps.MapleDoor;
import com.lucianms.server.maps.MapleMapObject;
import tools.MaplePacketCreator;

import java.util.List;

/**
 *
 * @author izarooni
 */
public class PlayerMagicDoorUseEvent extends PacketEvent {

    private int playerID;
    private boolean townDestination;

    @Override
    public void processInput(MaplePacketReader reader) {
        playerID = reader.readInt();
        townDestination = reader.readByte() == 1;
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        boolean isPartyLeaderDoor = false;
        List<MapleDoor> doors = player.getDoors();

        if (playerID != player.getId()) {
            if (player.getParty() != null) {
                if (player.getParty().getLeaderPlayerID() == playerID) {
                    doors = player.getParty().getLeader().getDoors();
                    isPartyLeaderDoor = true;
                }
            }
        }

        if (!doors.isEmpty()) {
            for (MapleDoor door : doors) {
                MapleMapObject mapObject = player.getMap().getMapObject(door.getObjectId());
                if (mapObject != null) {
                    if (door.getOwner().getId() == playerID || isPartyLeaderDoor) {
                        if (player.getMap().isTown() ^ townDestination) {
                            door.warp(player, townDestination);
                            return null;
                        }
                    }
                }
            }
        }
        getClient().announce(MaplePacketCreator.enableActions());
        return null;
    }
}
