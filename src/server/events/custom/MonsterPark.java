package server.events.custom;

import client.MapleCharacter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scheduler.Task;
import scheduler.TaskExecutor;
import server.MaplePortal;
import server.life.*;
import server.maps.MapleMap;
import server.maps.MapleMapFactory;
import tools.MaplePacketCreator;

import java.util.Hashtable;

/**
 * @author izarooni
 */
public class MonsterPark extends GenericEvent {

    private static final Logger LOGGER = LoggerFactory.getLogger(MonsterPark.class);
    private static final int Stages = 5;
    private static final int Increments = 100;

    private final int mapId;
    private final MapleMapFactory mapFactory;
    private long timestampStart = 0;
    private Task timeout = null;
    private Hashtable<Integer, Integer> returnMaps = new Hashtable<>();

    public MonsterPark(int world, int channel, int mapId) {
        this.mapId = mapId;
        this.mapFactory = new MapleMapFactory(world, channel);

        for (int i = mapId; i < (mapId + (Stages * Increments)); i += Increments) {
            MapleMap instanceMap = mapFactory.skipMonsters(true).getMap(i);
            if (instanceMap != null) {

                final MaplePortal portal;
                if (instanceMap.getPortal("next00") != null) {
                    (portal = instanceMap.getPortal("next00")).setPortalStatus(false);
                } else {
                    portal = instanceMap.getPortal("final00");
                    if (portal != null) {
                        portal.setPortalState(false);
                    }
                }
                boolean finalMap = i == (mapId + Stages);

                for (SpawnPoint spawnPoint : instanceMap.getMonsterSpawnPoints()) {
                    MapleMonster monster = MapleLifeFactory.getMonster(spawnPoint.getMonster().getId());
                    if (monster != null) {
                        MapleMonsterStats stats = new MapleMonsterStats();
                        stats.setHp(monster.getHp());
                        stats.setMp(monster.getMp());
                        stats.setExp((int) (stats.getExp() * 1.25));
                        monster.setOverrideStats(stats);
                        monster.addListener(new MonsterListener() {
                            @Override
                            public void monsterKilled(int aniTime) {
                                MapleMap currentMap = monster.getMap();
                                if (currentMap.getMonsters().isEmpty()) {
                                    if (finalMap) {
                                        timeout.cancel();
                                        currentMap.broadcastMessage(MaplePacketCreator.showEffect("monsterPark/clearF"));
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
                                        currentMap.broadcastMessage(MaplePacketCreator.showEffect("monsterPark/clear"));
                                    }
                                }
                            }
                        });
                    }
                    instanceMap.spawnMonsterOnGroudBelow(monster, spawnPoint.getPosition());
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
        player.announce(MaplePacketCreator.showEffect("monsterPark/stageEff/number/1"));
        player.addGenericEvent(this);
    }

    @Override
    public void unregisterPlayer(MapleCharacter player) {
        player.removeGenericEvent(this);
        player.changeMap(returnMaps.remove(player.getId()));
    }

    public void advanceMap(MapleCharacter player) {
        if (player.getMapId() == getMapId() + (Stages * Increments)) {
            unregisterPlayer(player);
        } else {
            int stage = ((player.getMapId() + Increments) % 1000 / 100);

            // player is still in the monster park area
            if (player.getMapId() >= mapId && player.getMapId() <= (mapId + (Stages * Increments))) {
                player.changeMap(mapFactory.getMap(player.getMap().getId() + Increments));
                // (start timestamp + 20min) = timeout
                // timeout - (current time) = elapsed time (aka: seconds left)
                int sLeft = (int) (((timestampStart + (1000 * 60 * 20)) - System.currentTimeMillis()) / 1000);
                player.announce(MaplePacketCreator.getClock(sLeft));
                if (stage == 5) { // proceeding to final stage; stage 6
                    player.announce(MaplePacketCreator.showEffect("monsterPark/stageEff/final"));
                } else {
                    player.announce(MaplePacketCreator.showEffect("monsterPark/stageEff/stage"));
                    player.announce(MaplePacketCreator.showEffect("monsterPark/stageEff/number/" + (stage + 1)));
                }
            } else {
                unregisterPlayer(player);
            }
        }
    }

    public int getMapId() {
        return mapId;
    }
}
