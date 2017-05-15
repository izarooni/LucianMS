package server.events.custom.auto;

import client.MapleCharacter;
import net.server.world.World;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.life.MapleMonsterStats;
import server.life.MonsterListener;
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

    private static final int EventMap = 910040002;
    private static final int MonsterId = 100100;
    private static final int xSpawn = 68, ySpawn = 155;
    private static final int SixMinutes = (1000 * 60) * 6; // :d

    private long startTimestamp = 0L;

    private int objectId = -1;

    private Task timeoutTask = null;

    private HashMap<Integer, Integer> returnMaps = new HashMap<>();

    public AWhispyWoods(World world) {
        super(world, true);
    }

    @Override
    public void start() {
        broadcastWorldMessage("Whispy Woods will begin momentarily");
        createTask(new Runnable() {
            @Override
            public void run() {
                startTimestamp = System.currentTimeMillis();
                summonBoss();
            }
        }, 5000);
    }

    @Override
    public void stop() {
        super.dispose();
        for (MapleCharacter players : getPlayers()) {
            unregisterPlayer(players);
        }
    }

    @Override
    public void playerRegistered(MapleCharacter player) {
        // show countdown timer to those entering late
        long endTimestamp = startTimestamp + SixMinutes;
        long timeLeft = (endTimestamp - System.currentTimeMillis());
        if (timeLeft > 0) {
            returnMaps.put(player.getId(), player.getMapId());
            player.dropMessage("Welcome to Whispy Woods!");
            player.changeMap(getMapInstance(EventMap));
            player.addGenericEvent(this);
            player.announce(MaplePacketCreator.getClock((int) (timeLeft / 1000)));
        } else {
            player.dropMessage(5, "This event is now over");
        }
    }

    @Override
    public void playerUnregistered(MapleCharacter player) {
        player.removeGenericEvent(this);
        int returnMap = returnMaps.remove(player.getId());
        player.changeMap(returnMap);
    }

    private void summonBoss() {
        getMapInstance(EventMap).broadcastMessage(MaplePacketCreator.getClock(SixMinutes / 1000));

        timeoutTask = createTask(new Runnable() {
            @Override
            public void run() {
                eventFailed();
            }
        }, SixMinutes);
        MapleMonster monster = MapleLifeFactory.getMonster(MonsterId);
        if (monster != null) {
            MapleMonsterStats stats = new MapleMonsterStats();
            stats.setHp(Integer.MAX_VALUE);
            monster.setOverrideStats(stats);
            monster.addListener(new MonsterListener() {
                @Override
                public void monsterKilled(int aniTime) {
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
