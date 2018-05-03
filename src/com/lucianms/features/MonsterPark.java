package com.lucianms.features;

import client.MapleCharacter;
import constants.ExpTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.lucianms.scheduler.Task;
import com.lucianms.scheduler.TaskExecutor;
import server.MaplePortal;
import server.life.*;
import server.maps.MapleMap;
import server.maps.MapleMapFactory;
import tools.MaplePacketCreator;

import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author izarooni
 */
public class MonsterPark extends GenericEvent {

    private static final Logger LOGGER = LoggerFactory.getLogger(MonsterPark.class);
    private static final int Stages = 5;
    private static final int Increment = 100;

    private final AtomicInteger totalExp = new AtomicInteger(0);
    private final int mapId;
    private final MapleMapFactory mapFactory;
    private long timestampStart = 0;
    private Task timeout = null;
    private Hashtable<Integer, Integer> returnMaps = new Hashtable<>();

    public MonsterPark(int world, int channel, int mapId, int baseLevel) {
        this.mapId = mapId;
        this.mapFactory = new MapleMapFactory(world, channel);

        for (int i = mapId; i < (mapId + (Stages * Increment)); i += Increment) {
            MapleMap instanceMap = mapFactory.skipMonsters(true).getMap(i);
            if (instanceMap != null) {

                final MaplePortal portal;
                if (instanceMap.getPortal("next00") != null) {
                    (portal = instanceMap.getPortal("next00")).setPortalStatus(false);
                } else {
                    portal = instanceMap.getPortal("final00");
                    if (portal != null) {
                        portal.setPortalStatus(false);
                    }
                }
                for (SpawnPoint spawnPoint : instanceMap.getMonsterSpawnPoints()) {
                    MapleMonster monster = MapleLifeFactory.getMonster(spawnPoint.getMonster().getId());
                    if (monster != null) {
                        MapleMonsterStats stats = new MapleMonsterStats();
                        stats.setHp(monster.getHp());
                        stats.setMp(monster.getMp());
                        stats.setExp((int) (ExpTable.getExpNeededForLevel(baseLevel) * (Math.random() * 0.01) + 0.01));
                        monster.setOverrideStats(stats);
                        monster.addListener(new MonsterListener() {
                                @Override
                            public void monsterKilled(int aniTime) {
                                totalExp.addAndGet(monster.getExp());
                                if (instanceMap.getMonsters().isEmpty()) {
                                    if (getStage(instanceMap.getId()) == 5) {
                                        timeout.cancel();
                                        instanceMap.broadcastMessage(MaplePacketCreator.showEffect("monsterPark/clearF"));
                                        TaskExecutor.createTask(new Runnable() {
                                            @Override
                                            public void run() {
                                                for (int i = mapId; i < (mapId + Stages); i++) {
                                                    MapleMap instanceMap = mapFactory.getMap(i);
                                                    instanceMap.getAllPlayer().forEach(MonsterPark.this::unregisterPlayer);
                                                }
                                            }
                                        }, 3500);
                                    } else {
                                        if (portal != null) {
                                            portal.setPortalStatus(true);
                                        }
                                        instanceMap.broadcastMessage(MaplePacketCreator.showEffect("monsterPark/clear"));
                                    }
                                }
                            }
                        });
                        instanceMap.spawnMonsterOnGroudBelow(monster, spawnPoint.getPosition());
                    } else {
                        LOGGER.warn("Invalid monster {} for map {}", spawnPoint.getMonster().getId(), mapId);
                    }
                }
            } else {
                LOGGER.warn("Invalid map {}", i);
            }
        }
    }

    @Override
    public void registerPlayer(MapleCharacter player) {
        if (timeout == null) { // initialization
            timeout = TaskExecutor.createTask(() -> returnMaps.forEach((p, m) -> unregisterPlayer(player)), 1000 * 60 * 20);
            timestampStart = System.currentTimeMillis();
        }
        returnMaps.put(player.getId(), player.getMapId());
        player.changeMap(mapFactory.getMap(mapId));
        player.announce(MaplePacketCreator.getClock((60 * 20)));
        player.announce(MaplePacketCreator.showEffect("monsterPark/stageEff/stage"));
        TaskExecutor.createTask(() -> player.announce(MaplePacketCreator.showEffect("monsterPark/stageEff/number/1")), 2345);
        player.addGenericEvent(this);
    }

    @Override
    public void unregisterPlayer(MapleCharacter player) {
        player.removeGenericEvent(this);
        player.changeMap(returnMaps.remove(player.getId()));
    }

    @Override
    public boolean banishPlayer(MapleCharacter player, int mapId) {
        return false;
    }

    private int getStage(int mapId) {
        return mapId % 1000 / 100;
    }

    public void advanceMap(MapleCharacter player) {
        if (player.getMapId() == getMapId() + (Stages * Increment)) {
            // final stage
            player.gainExp((int) (totalExp.get() * 1.1), true, true);
            unregisterPlayer(player);
        } else {
            final int cMapId = player.getMapId();
            int stage = getStage(cMapId);

            // player is still in the monster park area
            if (cMapId >= mapId && cMapId <= (mapId + (Stages * Increment))) {
                player.changeMap(mapFactory.getMap(cMapId + Increment));
                // (start timestamp + 20min) = timeout
                // timeout - (current time) = elapsed time (aka: seconds left)
                int sLeft = (int) (((timestampStart + (1000 * 60 * 20)) - System.currentTimeMillis()) / 1000);
                player.announce(MaplePacketCreator.getClock(sLeft));
                if (stage == 4) { // proceeding to final stage; stage 6
                    player.announce(MaplePacketCreator.showEffect("monsterPark/stageEff/final"));
                } else {
                    // (stage + 2) because map index beings at 0 (advancing maps requires +1 to the ID)
                    // but in game (front end), stages being at 1 so advancing to stage 2 we must increment by 2
                    player.announce(MaplePacketCreator.showEffect("monsterPark/stageEff/stage"));
                    TaskExecutor.createTask(() -> player.announce(MaplePacketCreator.showEffect("monsterPark/stageEff/number/" + (stage + 2))), 2345);
                }
            } else {
                // player is in another map?
                unregisterPlayer(player);
            }
        }
    }

    public int getMapId() {
        return mapId;
    }
}
