package com.lucianms.features;

import client.MapleCharacter;
import com.lucianms.io.scripting.npc.NPCScriptManager;
import com.lucianms.scheduler.Task;
import com.lucianms.scheduler.TaskExecutor;
import constants.ExpTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.FieldBuilder;
import server.MaplePortal;
import server.life.MapleMonster;
import server.life.MapleMonsterStats;
import server.life.MonsterListener;
import server.life.SpawnPoint;
import server.maps.MapleMap;
import tools.MaplePacketCreator;

import java.util.HashMap;
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
    private final HashMap<Integer, MapleMap> maps = new HashMap<>(5);
    private long timestampStart = 0;
    private Task timeout = null;
    private Hashtable<Integer, Integer> returnMaps = new Hashtable<>();

    public MonsterPark(int worldID, int channelID, int mapID, int baseLevel) {
        this.mapId = mapID;

        for (int i = mapID; i <= (mapID + (Stages * Increment)); i += Increment) {
            MapleMap instanceMap = new FieldBuilder(worldID, channelID, i).loadFootholds().loadPortals().build();
            if (instanceMap != null) {
                instanceMap.setRespawnEnabled(false);
                maps.put(i, instanceMap);
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
                    MapleMonster monster = spawnPoint.getMonster();
                    if (monster != null) {
                        MapleMonsterStats overrides = spawnPoint.createOverrides();
                        overrides.setExp((int) (ExpTable.getExpNeededForLevel(baseLevel) * (Math.random() * 0.01) + 0.01));
                        monster.getListeners().add(new MonsterListener() {
                            @Override
                            public void monsterKilled(int aniTime) {
                                if (portal != null) {
                                    portal.setPortalStatus(true);
                                }
                                totalExp.addAndGet(monster.getExp());
                                if (instanceMap.getMonsters().stream().noneMatch(m -> m.getHp() > 0)) {
                                    if (getStage(instanceMap.getId()) == 5) {
                                        timeout.cancel();
                                        instanceMap.broadcastMessage(MaplePacketCreator.showEffect("monsterPark/clearF"));
                                        TaskExecutor.createTask(new Runnable() {
                                            @Override
                                            public void run() {
                                                for (int i = mapID; i < (mapID + Stages); i++) {
                                                    MapleMap instanceMap = maps.get(i);
                                                    if (instanceMap != null) {
                                                        instanceMap.getAllPlayer().forEach(MonsterPark.this::unregisterPlayer);
                                                    }
                                                }
                                            }
                                        }, 3500);
                                    } else {
                                        instanceMap.broadcastMessage(MaplePacketCreator.showEffect("monsterPark/clear"));
                                    }
                                }
                            }
                        });
                        spawnPoint.summonMonster();
                        instanceMap.spawnMonsterOnGroudBelow(monster, spawnPoint.getPosition());
                    } else {
                        LOGGER.warn("Invalid monster {} for map {}", spawnPoint.getMonsterID(), mapID);
                    }
                }
            } else {
                LOGGER.warn("Invalid map {}", i);
            }
        }
    }

    @Override
    public void registerPlayer(MapleCharacter player) {
        if (player.addGenericEvent(this)) {
            if (timeout == null) { // initialization
                timeout = TaskExecutor.createTask(() -> returnMaps.forEach((p, m) -> unregisterPlayer(player)), 1000 * 60 * 20);
                timestampStart = System.currentTimeMillis();
            }
            returnMaps.put(player.getId(), player.getMapId());
            player.changeMap(maps.get(mapId));
            player.announce(MaplePacketCreator.getClock((60 * 20)));
            player.announce(MaplePacketCreator.showEffect("monsterPark/stageEff/stage"));
            TaskExecutor.createTask(() -> player.announce(MaplePacketCreator.showEffect("monsterPark/stageEff/number/1")), 2345);
        }
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

    @Override
    public boolean onPlayerChangeMapInternal(MapleCharacter player, MapleMap destination) {
        if (player.getMapId() / 1000000 == 95) {
            NPCScriptManager.start(player.getClient(), 9071000, "f_monster_park_quit");
            return false;
        }
        return true;
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
                player.changeMap(maps.get(cMapId + Increment));
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
