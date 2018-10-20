package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.maps.FieldLimit;
import tools.MaplePacketCreator;

/**
 * @author kevintjuh93
 * @author izarooni
 */
public class PlayerTeleportRockRecordEvent extends PacketEvent {

    private byte type;
    private int fieldID;
    private boolean vip;

    @Override
    public void processInput(MaplePacketReader reader) {
        type = reader.readByte();
        vip = reader.readByte() == 1;
        if (type == 0) {
            fieldID = reader.readInt();
        }
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        if (type == 0x00) {
            if (vip) {
                player.deleteFromVipTrocks(fieldID);
            } else {
                player.deleteFromTrocks(fieldID);
            }
            getClient().announce(MaplePacketCreator.trockRefreshMapList(player, true, vip));
        } else if (type == 0x01) {
            if (!FieldLimit.CANNOTVIPROCK.check(player.getMap().getFieldLimit())) {
                if (vip) {
                    player.addVipTrockMap();
                } else {
                    player.addTrockMap();
                }
                getClient().announce(MaplePacketCreator.trockRefreshMapList(player, false, vip));
            } else {
                player.message("You may not save this map.");
            }
        }
        return null;
    }
}
