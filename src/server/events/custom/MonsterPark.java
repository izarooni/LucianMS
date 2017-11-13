package server.events.custom;

import client.MapleCharacter;
import net.server.channel.handlers.ChangeMapHandler;
import scheduler.Task;
import scheduler.TaskExecutor;
import server.life.MapleMonster;
import server.life.MapleMonsterStats;
import server.life.MonsterListener;
import server.maps.MapleMap;
import server.maps.MapleMapFactory;
import tools.MaplePacketCreator;
import tools.annotation.PacketWorker;

import java.util.Hashtable;

/**
 * @author izarooni
 */
public class MonsterPark extends GenericEvent {

    private final int mapId;
    private final MapleMapFactory mapFactory;
    private boolean canContinue = false;
    private Task timeout = null;
    private Hashtable<Integer, Integer> returnMaps = new Hashtable<>();

    public MonsterPark(int world, int channel, int mapId) {
        registerAnnotationPacketEvents(this);
        this.mapId = mapId;
        this.mapFactory = new MapleMapFactory(world, channel);
    }

    @Override
    public void registerPlayer(MapleCharacter player) {
        returnMaps.put(player.getId(), player.getMapId());
        player.changeMap(mapId);
        player.addGenericEvent(this);
        timeout = TaskExecutor.createTask(new Runnable() {
            @Override
            public void run() {

            }
        }, 1000 * 60 * 20);
    }

    @Override
    public void unregisterPlayer(MapleCharacter player) {
        player.removeGenericEvent(this);
        player.changeMap(returnMaps.remove(player.getMapId()));
    }

    @PacketWorker
    public void onChangeMap(ChangeMapHandler event) {
        MapleCharacter player = event.getClient().getPlayer();

        int targetMap = event.getTargetMapId();
        int stage = (mapId + 6) - targetMap;
        boolean finalMap = targetMap == (mapId + 6);

        if (targetMap > mapId && targetMap <= (mapId + 6)) {
            if (finalMap) {
                player.announce(MaplePacketCreator.showEffect("monsterPark/stageEff/final"));
            } else {
                player.announce(MaplePacketCreator.showEffect("monsterPark/stageEff/stage"));
                player.announce(MaplePacketCreator.showEffect("monsterPark/stageEff/number/" + stage));
            }
            boolean loaded = mapFactory.isMapLoaded(targetMap);
            MapleMap instanceMap = mapFactory.getMap(targetMap);
            instanceMap.toggleDrops();
            if (!loaded) {
                for (MapleMonster monster : instanceMap.getMonsters()) {
                    MapleMonsterStats stats = new MapleMonsterStats();
                    stats.setExp((int) (stats.getExp() * 1.2));
                    monster.setOverrideStats(stats);
                    monster.addListener(new MonsterListener() {
                        @Override
                        public void monsterKilled(int aniTime) {
                            MapleMap currentMap = player.getMap();
                            if (currentMap.getMonsters().isEmpty()) {
                                canContinue = true;
                                if (finalMap) {
                                    timeout.cancel();
                                    player.announce(MaplePacketCreator.showEffect("monsterPark/clearF"));
                                    TaskExecutor.createTask(new Runnable() {
                                        @Override
                                        public void run() {
                                            returnMaps.forEach((p, m) -> currentMap.getCharacterById(p).changeMap(m));
                                        }
                                    }, 3500);
                                } else {
                                    player.announce(MaplePacketCreator.showEffect("monsterPark/clear"));
                                }
                            }
                        }
                    });
                }
            }
            player.changeMap(instanceMap);
        } else {
            unregisterPlayer(player);
        }
        event.setCanceled(true);
    }

    public int getMapId() {
        return mapId;
    }
}
