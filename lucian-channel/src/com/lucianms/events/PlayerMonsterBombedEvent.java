package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.life.MapleMonster;
import tools.MaplePacketCreator;

/**
 * @author izarooni
 */
public class PlayerMonsterBombedEvent extends PacketEvent {

    private int objectID;

    @Override
    public void processInput(MaplePacketReader reader) {
        objectID = reader.readInt();
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        MapleMonster monster = player.getMap().getMonsterByOid(objectID);
        if (!player.isAlive() || monster == null) {
            return null;
        }
        if (monster.getId() == 8500003 || monster.getId() == 8500004) {
            monster.getMap().broadcastMessage(MaplePacketCreator.killMonster(monster.getObjectId(), 4));
            player.getMap().removeMapObject(objectID);
        }
        return null;
    }
}
