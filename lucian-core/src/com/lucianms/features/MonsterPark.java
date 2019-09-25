package com.lucianms.features;

import com.lucianms.client.MapleCharacter;
import com.lucianms.constants.ExpTable;
import com.lucianms.io.scripting.npc.NPCScriptManager;
import com.lucianms.scheduler.Task;
import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.server.FieldBuilder;
import com.lucianms.server.MaplePortal;
import com.lucianms.server.life.MapleMonster;
import com.lucianms.server.life.MapleMonsterStats;
import com.lucianms.server.life.MonsterListener;
import com.lucianms.server.life.SpawnPoint;
import com.lucianms.server.maps.MapleMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public MonsterPark(int worldID, int channelID, final int mapID, int baseLevel) {
        this.mapId = mapID;

        for (int i = mapID; i <= (mapID + (Stages * Increment)); i += Increment) {
            MapleMap instanceMap = new FieldBuilder(worldID, channelID, i).loadFootholds().loadPortals().build();
            if (instanceMap != null) {
                instanceMap.setInstanced(true);
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
                    MapleMonsterStats overrides = spawnPoint.createOverrides();
                    overrides.setExp((int) (ExpTable.getExpNeededForLevel(baseLevel) * (Math.random() * 0.01) + 0.01));
                    MapleMonster monster = spawnPoint.summonMonster();
                    if (monster != null) {
                        monster.getListeners().add(new MPMonsterHandler(portal));
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
            player.sendMessage(5, "You may leave at any time using a warp command.");
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
        if (!destination.isInstanced()) {
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

    private class MPMonsterHandler extends MonsterListener {

        private MaplePortal portal;

        public MPMonsterHandler(MaplePortal portal) {
            this.portal = portal;
        }

        @Override
        public void monsterKilled(MapleMonster monster, MapleCharacter player) {
            MapleMap map = monster.getMap();

            totalExp.addAndGet(monster.getExp());
            if (map.getMonsters().stream().noneMatch(m -> m.getHp() > 0)) {
                if (portal != null) {
                    portal.setPortalStatus(true);
                }
                if (getStage(map.getId()) == 5) {
                    timeout.cancel();
                    map.broadcastMessage(MaplePacketCreator.showEffect("monsterPark/clearF"));
                    TaskExecutor.createTask(new Runnable() {
                        @Override
                        public void run() {
                            for (MapleMap map : maps.values()) {
                                if (map != null) {
                                    map.getAllPlayer().forEach(MonsterPark.this::unregisterPlayer);
                                }
                            }
                        }
                    }, 3500);
                } else {
                    map.broadcastMessage(MaplePacketCreator.showEffect("monsterPark/clear"));
                }
            }
        }
    }
}
