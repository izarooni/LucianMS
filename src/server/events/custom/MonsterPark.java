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
    private boolean canContinue = false; // todo use portal states
    private Task timeout = null;
    private Hashtable<MapleCharacter, Integer> returnMaps = new Hashtable<>();

    public MonsterPark(int world, int channel, int mapId) {
        registerAnnotationPacketEvents(this);
        this.mapId = mapId;
        this.mapFactory = new MapleMapFactory(world, channel);
    }

    @Override
    public void registerPlayer(MapleCharacter player) {
        if (timeout == null) { // initialization
            timeout = TaskExecutor.createTask(() -> returnMaps.forEach((p, m) -> unregisterPlayer(player)), 1000 * 60 * 20);

            for (int i = mapId; i < (mapId + 6); i++) {
                MapleMap instanceMap = mapFactory.getMap(i);
                boolean finalMap = i == (mapId + 6);

                instanceMap.toggleDrops();
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
                                            returnMaps.forEach((p, m) -> unregisterPlayer(player));
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
        }
        returnMaps.put(player, player.getMapId());
        player.changeMap(mapId);
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
            int stage = (mapId + 6) - targetMap;
            boolean finalMap = targetMap == (mapId + 6);

            if (targetMap > mapId && targetMap <= (mapId + 6)) {
                if (finalMap) {
                    player.announce(MaplePacketCreator.showEffect("monsterPark/stageEff/final"));
                } else {
                    player.announce(MaplePacketCreator.showEffect("monsterPark/stageEff/stage"));
                    player.announce(MaplePacketCreator.showEffect("monsterPark/stageEff/number/" + stage));
                }
                MapleMap instanceMap = mapFactory.getMap(targetMap);
                player.changeMap(instanceMap);
                canContinue = false;
            } else {
                unregisterPlayer(player);
            }
        } else {
            player.dropMessage("You must clear all monsters on the map before proceeding");
        }
        event.setCanceled(true);
    }

    public int getMapId() {
        return mapId;
    }
}
