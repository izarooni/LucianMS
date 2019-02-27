package com.lucianms.features.emergency;

import com.lucianms.client.MapleCharacter;
import com.lucianms.server.life.*;
import com.lucianms.server.maps.MapleMap;
import com.lucianms.server.world.MapleParty;
import tools.MaplePacketCreator;

/**
 * @author izarooni
 */
public class EmergencyDuel extends Emergency {

    private static final int MinimumLevel = 30;
    private static final int LevelRangeIncrement = 20;
    private static final int[][] Bosses = new int[][]{
            // monster_id, exp_buff, hp_buff
            {3220000, 150, 130}, // Lv. 30 - 50
            {6130101, 130, 200}, // Lv. 50 - 70
            {6300005, 240, 250}, // Lv. 70 - 90
            {6400005, 180, 240}, // Lv. 90 - 110
            {8150000, 200, 270}, // Lv. 110 - 130
            {8220006, 130, 300}, // Lv. 130 - 150
            {8220005, 140, 250}, // Lv. 150 - 170
            {7220005, 300, 500}, // Lv. 170 - 190
    };

    public EmergencyDuel(MapleCharacter player) {
        super(player);

        MapleParty party = player.getParty();
        delay = (party != null && party.getMembers().size() >= 2) ? 800 : 300;
    }

    @Override
    public boolean banishPlayer(MapleCharacter player, int mapId) {
        return false;
    }

    @Override
    public void registerPlayer(MapleCharacter player) {
        if (!super.registerPlayers(player)) {
            return;
        }
        int index = (int) Math.floor((player.getLevel() - MinimumLevel) / (float) LevelRangeIncrement);
        if (index >= Bosses.length) {
            index = Bosses.length - 1;
        }
        summonBoss(index);
    }

    @Override
    public void unregisterPlayer(MapleCharacter player) {
        player.removeGenericEvent(this);
    }

    private void summonBoss(int index) {
        MapleMonster monster = MapleLifeFactory.getMonster(Bosses[index][0]);
        if (monster != null) {
            SpawnPoint spawnPoint = getMap().getMonsterSpawnPoints().stream().findAny().orElse(null);
            if (spawnPoint != null) {

                getMap().broadcastMessage(MaplePacketCreator.showEffect("PSO2/stuff/4"));
                getMap().broadcastMessage(MaplePacketCreator.playSound("PSO2/Duel"));

                MapleMonsterStats stats = new MapleMonsterStats();
                stats.setHp((int) (monster.getHp() * (Bosses[index][2] / 100d)));
                stats.setMp(monster.getMp());
                stats.setExp((int) (monster.getExp() * (Bosses[index][1] / 100d)));

                monster.getListeners().add(new EmergencyMobHandler());
                monster.setOverrideStats(stats);

                getMap().spawnMonsterOnGroudBelow(monster, spawnPoint.getPosition());
                getMap().broadcastMessage(MaplePacketCreator.earnTitleMessage(monster.getName() + " has spawned somewhere on this map!"));
            } else {
                logger().warn("Unable to spawn monster due to no spawn points available on map {}", getMap().getId());
            }
        } else {
            logger().warn("Invalid monster {}", Bosses[index][0]);
        }
    }

    private class EmergencyMobHandler extends MonsterListener {
        @Override
        public void monsterKilled(MapleMonster monster, MapleCharacter player) {
            cancelTimeout();
            unregisterPlayers();
            getMap().setRespawnEnabled(true);
            getMap().respawn();
            getMap().broadcastMessage(MaplePacketCreator.removeClock());
            getMap().broadcastMessage(MaplePacketCreator.showEffect("PSO2/stuff/5"));
            getMap().broadcastMessage(MaplePacketCreator.playSound("PSO2/Completed"));
        }

        @Override
        public MonsterDropEntry onDeathDrop(MapleMonster monster, MapleCharacter player) {
            return new MonsterDropEntry(4011034, MapleMap.MAX_DROP_CHANCE, 1, 1, (short) -1);
        }
    }
}
