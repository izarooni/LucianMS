package server.events.custom;

import client.MapleCharacter;
import net.server.channel.handlers.ChangeMapHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scheduler.Task;
import scheduler.TaskExecutor;
import server.life.*;
import server.maps.MapleMap;
import server.maps.MapleMapFactory;
import tools.MaplePacketCreator;
import tools.annotation.PacketWorker;

import java.util.Hashtable;

/**
 * @author izarooni
 */
public class MonsterPark extends GenericEvent {

    private static final Logger LOGGER = LoggerFactory.getLogger(MonsterPark.class);
    private static final int Stages = 6;
    private static final int Increments = 100;

    private final int mapId;
    private final MapleMapFactory mapFactory;
    private long timestampStart = 0;
    private boolean canContinue = false; // todo use portal states
    private Task timeout = null;
    private Hashtable<MapleCharacter, Integer> returnMaps = new Hashtable<>();

    public MonsterPark(int world, int channel, int mapId) {
        registerAnnotationPacketEvents(this);
        this.mapId = mapId;
        this.mapFactory = new MapleMapFactory(world, channel);

        for (int i = mapId; i < (mapId + (Stages * Increments)); i += Increments) {
            MapleMap instanceMap = mapFactory.skipMonsters(true).getMap(i);
            if (instanceMap != null) {
                LOGGER.info("{} loaded with {} monsters", mapId, instanceMap.getMonsters().size());
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
                                    canContinue = true;
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
        returnMaps.put(player, player.getMapId());
        player.changeMap(mapFactory.getMap(mapId));
        player.announce(MaplePacketCreator.getClock((int) ((timestampStart + (60 * 20)) - System.currentTimeMillis())));
        player.addGenericEvent(this);
    }

    @Override
    public void unregisterPlayer(MapleCharacter player) {
        player.removeGenericEvent(this);
        player.changeMap(returnMaps.remove(player));
    }

    @PacketWorker
    public void onChangeMap(ChangeMapHandler event) {
        MapleCharacter player = event.getClient().getPlayer();

        if (canContinue) {
            int targetMap = event.getTargetMapId();
            int stage = (mapId + Stages) - targetMap;
            boolean finalMap = targetMap == (mapId + Stages);

            if (targetMap > mapId && targetMap <= (mapId + Stages)) {
                if (finalMap) {
                    player.announce(MaplePacketCreator.showEffect("monsterPark/stageEff/final"));
                } else {
                    player.announce(MaplePacketCreator.showEffect("monsterPark/stageEff/stage"));
                    player.announce(MaplePacketCreator.showEffect("monsterPark/stageEff/number/" + stage));
                }
                MapleMap instanceMap = mapFactory.getMap(targetMap);
                player.changeMap(instanceMap);
                player.announce(MaplePacketCreator.getClock((int) ((timestampStart + (60 * 20)) - System.currentTimeMillis())));
                canContinue = false;
            } else {
                unregisterPlayer(player);
            }
        } else {
            player.dropMessage("You must clear all monsters on the map before proceeding");
            player.announce(MaplePacketCreator.enableActions());
        }
        event.setCanceled(true);
    }

    public int getMapId() {
        return mapId;
    }
}
