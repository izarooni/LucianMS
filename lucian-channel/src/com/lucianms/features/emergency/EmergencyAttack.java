package com.lucianms.features.emergency;

import com.lucianms.client.MapleCharacter;
import com.lucianms.server.MapleInventoryManipulator;
import com.lucianms.server.life.MapleLifeFactory;
import com.lucianms.server.life.MapleMonster;
import com.lucianms.server.life.MonsterListener;
import com.lucianms.server.life.SpawnPoint;
import com.lucianms.server.world.MapleParty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.MaplePacketCreator;
import tools.Randomizer;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author izarooni
 */
public class EmergencyAttack extends Emergency {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmergencyAttack.class);
    private static final int[][] monsters = new int[][]{
            {20, 9400638}, {25, 2100103}, {30, 3000005}, {35, 3230200},
            {40, 9400517}, {45, 4130100}, {50, 5120503}, {55, 5130101},
            {60, 9420511}, {65, 9420534}, {70, 9400640}, {75, 7130200},
            {80, 9400545}, {85, 7130010}, {90, 8140700}, {95, 9895239},
            {100, 9895240}, {105, 8200005}, {110, 8190003}, {115, 8200008},
            {120, 8200009}, {125, 8200011}, {130, 8200012}, {135, 8600000},
            //{140, }, {145, },
            {150, 9400112}, {155, 9400113},
            {160, 8642003},
            //{165, },
            {170, 8610006},
            //{175, },
            //{180, }, {185, },
            {190, 8620000}, {195, 8620007},
            {200, 8620009},
    };
    private AtomicInteger summoned = null;
    private int totalExp = 0;

    public EmergencyAttack(MapleCharacter player) {
        super(player);
        registerAnnotationPacketEvents(this);

        MapleParty party = player.getParty();
        delay = (party != null && party.size() >= 2) ? 60 : 120;
    }

    @Override
    public void registerPlayer(MapleCharacter player) {
        if (!registerPlayers(player)) {
            return;
        }
        summonMonsters(getAverageLevel());
    }

    @Override
    public void unregisterPlayer(MapleCharacter player) {
        player.removeGenericEvent(this);
    }

    private void summonMonsters(int level) {
        SpawnPoint[] sp = getMap().getMonsterSpawnPoints().toArray(new SpawnPoint[0]);
        for (int i = 0; i < monsters.length; i++) {
            int[] stats = monsters[i];
            int[] next = (i == (monsters.length - 1)) ? null : monsters[i + 1];
            if ((i == 0 && stats[0] > level) || next == null || (level >= stats[0] && level <= next[0])) {
                final int summons = Math.min(30, sp.length);
                summoned = new AtomicInteger(summons);
                for (int j = 0; j < summons; j++) {
                    MapleMonster monster = MapleLifeFactory.getMonster(stats[1]);
                    if (monster != null) {
                        monster.getListeners().add(new MCarnivalMobHandler());
                        getMap().spawnMonsterOnGroudBelow(monster, sp[Randomizer.nextInt(sp.length)].getPosition());
                    } else {
                        summoned.decrementAndGet();
                    }
                }
                getMap().broadcastMessage(MaplePacketCreator.earnTitleMessage(String.format("%s monsters at level %s have appeared!", summons, stats[0])));
                getMap().broadcastMessage(MaplePacketCreator.showEffect("PSO2/stuff/3"));
                getMap().broadcastMessage(MaplePacketCreator.playSound("PSO2/Attack"));
                break;
            }
        }
    }

    private class MCarnivalMobHandler extends MonsterListener {
        @Override
        public void monsterKilled(MapleMonster monster, MapleCharacter player) {
            int remaining = summoned.decrementAndGet();
            if (remaining == 0) {
                cancelTimeout();
                totalExp *= 1.50;
                unregisterPlayers();
                getMap().setRespawnEnabled(true);
                getMap().respawn();
                getMap().broadcastMessage(MaplePacketCreator.removeClock());
                getMap().broadcastMessage(MaplePacketCreator.showEffect("PSO2/stuff/5"));
                getMap().broadcastMessage(MaplePacketCreator.playSound("PSO2/Completed"));
                for (MapleCharacter p : players) {
                    if (p != null) {
                        p.gainExp(totalExp, true, true);
                        MapleInventoryManipulator.addById(player.getClient(), 4011033, (short) 1);
                    }
                }
            } else {
                getMap().broadcastMessage(MaplePacketCreator.serverNotice(5, "[Emergency] There are " + remaining + " monsters left."));
            }
        }
    }
}
