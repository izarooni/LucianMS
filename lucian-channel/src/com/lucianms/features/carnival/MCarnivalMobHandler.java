package com.lucianms.features.carnival;

import com.lucianms.client.MapleCharacter;
import com.lucianms.server.life.MapleMonster;
import com.lucianms.server.life.MonsterDropEntry;
import com.lucianms.server.life.MonsterListener;
import com.lucianms.server.maps.MapleMap;

/**
 * @author izarooni
 */
public class MCarnivalMobHandler extends MonsterListener {

    @Override
    public void monsterKilled(MapleMonster monster, MapleCharacter player) {
        MCarnivalGame carnivalGame = (MCarnivalGame) player.getGenericEvents().stream().filter(o -> o instanceof MCarnivalGame).findFirst().orElse(null);
        if (monster.getCP() > 0 && carnivalGame != null) {
            carnivalGame.getTeam(player.getTeam()).addCarnivalPoints(player, monster.getCP());
            player.announce(MCarnivalPacket.getMonsterCarnivalPointsUpdate(player.getCP(), player.getObtainedCP()));
            player.getMap().broadcastMessage(MCarnivalPacket.getMonsterCarnivalPointsUpdateParty(carnivalGame.getTeam(player.getTeam())));
        }
    }

    @Override
    public MonsterDropEntry onDeathDrop(MapleMonster monster, MapleCharacter player) {
        return new MonsterDropEntry(4310020, MapleMap.MAX_DROP_CHANCE / 100 * 35, 1, 1, (short) -1);
    }
}
