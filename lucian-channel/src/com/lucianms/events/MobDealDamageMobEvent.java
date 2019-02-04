package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.events.PacketEvent;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.maps.MapleMap;

/**
 * @author Jay Estrella
 * @author izarooni
 */
public class MobDealDamageMobEvent extends PacketEvent {

    private int sourceID;
    private int targetID;
    private int damage;

    @Override
    public void processInput(MaplePacketReader reader) {
        sourceID = reader.readInt();
        reader.readInt();
        targetID = reader.readInt();
        reader.readByte();
        damage = reader.readInt();
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        MapleMap map = player.getMap();
        if (map.getMonsterByOid(sourceID) != null && map.getMonsterByOid(targetID) != null) {
            map.damageMonster(player, map.getMonsterByOid(targetID), damage);
        }
        return null;
    }
}
