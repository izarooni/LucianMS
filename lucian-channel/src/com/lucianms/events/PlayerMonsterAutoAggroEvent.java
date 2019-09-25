package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.events.PacketEvent;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.life.MapleMonster;

/**
 * @author izarooni
 */
public class PlayerMonsterAutoAggroEvent extends PacketEvent {

    private int objectID;

    @Override
    public void processInput(MaplePacketReader reader) {
        objectID = reader.readInt();
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        MapleMonster monster = player.getMap().getMonsterByOid(objectID);

        if (player.isHidden()) {
            return null;
        }

        if (monster != null && monster.getController() != null) {
            if (!monster.isControllerHasAggro()) {
                if (player.getMap().getCharacterById(monster.getController().getId()) == null) {
                    monster.switchController(player, true);
                } else {
                    monster.switchController(monster.getController(), true);
                }
            } else if (player.getMap().getCharacterById(monster.getController().getId()) == null) {
                monster.switchController(player, true);
            }
        } else if (monster != null) {
            monster.switchController(player, true);
        }
        return null;
    }
}
