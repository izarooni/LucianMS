package com.lucianms.features.auto;

import com.lucianms.client.MapleCharacter;
import com.lucianms.scheduler.Task;
import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.server.life.MapleLifeFactory;
import com.lucianms.server.life.MapleMonster;
import com.lucianms.server.life.MapleMonsterStats;
import com.lucianms.server.life.MonsterListener;
import com.lucianms.server.world.MapleWorld;
import tools.MaplePacketCreator;

import java.awt.*;
import java.util.HashMap;

/**
 * <p>
 * The objective of this event is to kill the summoned boss within 6 minutes
 * </p>
 *
 * @author izarooni
 */
public class AWhispyWoods extends GAutoEvent {

    private static final long TimeGiven = (1000 * 60) * 8; // :d

    private static final int EventMap = 4;
    private static final int MonsterId = 9895243;
    private static final int xSpawn = 68, ySpawn = 155;

    private long startTimestamp = 0L;

    private int objectId = -1;

    private Task timeoutTask = null;

    private HashMap<Integer, Integer> returnMaps = new HashMap<>();

    public AWhispyWoods(MapleWorld world) {
        super(world, true);
    }

    @Override
    public void start() {
        broadcastWorldMessage("Whispy Woods will begin momentarily");
        TaskExecutor.createTask(new Runnable() {
            @Override
            public void run() {
                startTimestamp = System.currentTimeMillis();
                summonBoss();
            }
        }, 5000);
    }

    @Override
    public void stop() {
        GAutoEventManager.setCurrentEvent(null);
        for (MapleCharacter players : getPlayers()) {
            unregisterPlayer(players);
        }
    }

    @Override
    public void playerRegistered(MapleCharacter player) {
        if (player.addGenericEvent(this)) {
            // show countdown timer to those entering late
            long endTimestamp = startTimestamp + TimeGiven;
            long timeLeft = (endTimestamp - System.currentTimeMillis());
            if (timeLeft > 0) {
                returnMaps.put(player.getId(), player.getMapId());
                player.dropMessage("Welcome to Whispy Woods!");
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

    private void summonBoss() {
        getMapInstance(EventMap).broadcastMessage(MaplePacketCreator.getClock((int) (TimeGiven / 1000)));

        timeoutTask = TaskExecutor.createTask(new Runnable() {
            @Override
            public void run() {
                eventFailed();
            }
        }, TimeGiven);
        MapleMonster monster = MapleLifeFactory.getMonster(MonsterId);
        if (monster != null) {
            MapleMonsterStats stats = new MapleMonsterStats();
            stats.setHp(Integer.MAX_VALUE);
            monster.setOverrideStats(stats);
            monster.getListeners().add(new MonsterListener() {
                @Override
                public void monsterKilled(MapleMonster monster, MapleCharacter player) {
                    timeoutTask.cancel();
                    distributeRewards();
                    stop();
                }
            });
            getMapInstance(EventMap).spawnMonsterOnGroudBelow(monster, new Point(xSpawn, ySpawn));
        } else {
            broadcastWorldMessage("Heartless Wall is being canceled due to an error");
            stop();
        }
    }

    private void eventFailed() {
        stop();
    }

    private void distributeRewards() {
        for (MapleCharacter players : getPlayers()) {
            players.gainMeso(1, true);
        }
    }
}
