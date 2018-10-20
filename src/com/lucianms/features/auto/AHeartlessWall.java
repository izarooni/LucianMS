package com.lucianms.features.auto;

import client.MapleCharacter;
import com.lucianms.lang.annotation.PacketWorker;
import com.lucianms.scheduler.Task;
import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.server.events.channel.AbstractDealDamageEvent;
import com.lucianms.server.events.channel.PlayerDealDamageNearbyEvent;
import com.lucianms.server.events.channel.PlayerDealDamageMagicEvent;
import com.lucianms.server.events.channel.PlayerDealDamageRangedEvent;
import net.server.world.MapleWorld;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.life.MapleMonsterStats;
import server.life.MonsterListener;
import server.maps.MapleMap;
import server.maps.MapleMapObject;
import tools.MaplePacketCreator;

import java.awt.*;
import java.util.HashMap;
import java.util.List;

/**
 * @author izarooni
 */
public class AHeartlessWall extends GAutoEvent {

    private static final long TimeGiven = (1000 * 60) * 15;

    private static final int EventMap = 910000025;
    private static final int TheWall = 9895215; // monster
    private static final int xSpawn = 1226, ySpawn = 56;

    private long startTimestamp = 0L;

    private int objectId = -1;

    private HashMap<Integer, Integer> returnMaps = new HashMap<>();
    private HashMap<Integer, Integer> damageLog = new HashMap<>();

    public AHeartlessWall(MapleWorld world) {
        super(world, true);
    }

    @Override
    public void start() {
        startTimestamp = System.currentTimeMillis();

        broadcastWorldMessage("Heartless Wall will begin momentarily");
        MapleMap eventMap = getMapInstance(EventMap);
        eventMap.killAllMonsters();
        TaskExecutor.createTask(new Runnable() {
            @Override
            public void run() {
                summonWall();
            }
        }, 10000);
    }

    @Override
    public void stop() {
        GAutoEventManager.setCurrentEvent(null);
        // all players to return to their original map
        // should the return map not exist, return to a town/home
        for (MapleCharacter players : getPlayers()) {
            unregisterPlayer(players);
        }
    }

    @Override
    public void playerRegistered(MapleCharacter player) {
        if (player.addGenericEvent(this)) {
            // show countdown timer to those entering late
            long endTimestamp = startTimestamp + 10000;
            long timeLeft = (endTimestamp - System.currentTimeMillis());
            if (timeLeft > 0) {
                player.dropMessage("Welcome to Heartless Wall!");
                returnMaps.put(player.getId(), player.getMapId());
                player.changeMap(getMapInstance(EventMap));
                player.announce(MaplePacketCreator.getClock((int) (timeLeft / 1000)));
            } else {
                player.dropMessage(5, "This event is now over");
            }
        }
    }

    @Override
    public void playerUnregistered(MapleCharacter player) {
        player.removeGenericEvent(this);
        if (returnMaps.containsKey(player.getId())) {
            int returnMap = returnMaps.remove(player.getId());
            player.changeMap(returnMap);
        }
    }

    @PacketWorker
    public void onCloseRangeAttack(PlayerDealDamageNearbyEvent event) {
        onMonsterAttacked(event.getClient().getPlayer(), event.getAttackInfo());
    }

    @PacketWorker
    public void onRangedAttack(PlayerDealDamageRangedEvent event) {
        onMonsterAttacked(event.getClient().getPlayer(), event.getAttackInfo());
    }

    @PacketWorker
    public void onMagicAttack(PlayerDealDamageMagicEvent event) {
        onMonsterAttacked(event.getClient().getPlayer(), event.getAttackInfo());
    }

    private void onMonsterAttacked(MapleCharacter player, AbstractDealDamageEvent.AttackInfo attackInfo) {
        if (objectId != -1) {
            MapleMap eventMap = getMapInstance(EventMap);
            MapleMapObject object = eventMap.getMapObject(objectId);
            if (object != null && object instanceof MapleMonster) {
                if (object.getObjectId() == objectId) {
                    MapleMonster monster = (MapleMonster) object;
                    if (monster.isAlive()) {
                        int totalDaamge = 0;
                        for (List<Integer> list : attackInfo.allDamage.values()) {
                            totalDaamge += list.stream().mapToInt(Integer::intValue).sum();
                        }
                        // statistics? idk none of this is necessary
                        damageLog.put(player.getId(), damageLog.getOrDefault(player.getId(), 0) + totalDaamge);
                    }
                }
            }
        }
    }

    private void summonWall() {
        MapleMap eventMap = getMapInstance(EventMap);

        Task timeoutTask = TaskExecutor.createTask(new Runnable() {
            @Override
            public void run() {
                MapleMapObject object = eventMap.getMapObject(objectId);
                if (object != null && object instanceof MapleMonster) {
                    MapleMonster monster = (MapleMonster) object;
                    if (monster.isAlive()) {
                        eventMap.broadcastMessage(MaplePacketCreator.serverNotice(5, "You are being moved due to being unable to defeat the wall"));
                        stop();
                    }
                }
            }
        }, TimeGiven);

        MapleMonster monster = MapleLifeFactory.getMonster(TheWall);
        if (monster != null) {
            MapleMonsterStats stats = new MapleMonsterStats();
            stats.setHp(Integer.MAX_VALUE);
            monster.setOverrideStats(stats);
            monster.getListeners().add(new MonsterListener() {
                @Override
                public void monsterKilled(int aniTime) {
                    timeoutTask.cancel();
                    distributeRewards();
                    stop();
                }
            });
            eventMap.spawnMonsterOnGroudBelow(monster, new Point(xSpawn, ySpawn));
            objectId = monster.getObjectId();
        } else {
            broadcastWorldMessage("Heartless Wall is being canceled due to an error");
            stop();
        }
    }

    private void distributeRewards() {
        for (MapleCharacter players : getPlayers()) {
            players.gainMeso(1, true);
        }
    }
}
