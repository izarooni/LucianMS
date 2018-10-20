package com.lucianms.features.scheduled;

import com.lucianms.client.MapleCharacter;
import com.lucianms.scheduler.Task;
import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.server.channel.MapleChannel;
import com.lucianms.server.life.MapleLifeFactory;
import com.lucianms.server.life.MapleMonster;
import com.lucianms.server.life.MonsterListener;
import com.lucianms.server.maps.MapleMap;
import com.lucianms.server.maps.SavedLocationType;
import com.lucianms.server.world.MapleWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.MaplePacketCreator;
import tools.Randomizer;

import java.awt.*;
import java.util.Arrays;

/**
 * Should only ever been one instance per world
 *
 * @author izarooni
 */
public class SOuterSpace extends SAutoEvent {

    private static final Logger LOGGER = LoggerFactory.getLogger(SOuterSpace.class);

    private static final int MapId = 98;
    private static final int MonsterId = 9895259;
    private static final int[][] PortalPositions = {{156, 880}, {2352, 881}, {3124, 581}, {3831, 821}};

    private final MapleWorld world;
    private final boolean[] finished;
    private Task timeoutTask = null;
    private long start;
    private boolean open = false;

    public SOuterSpace(MapleWorld world) {
        this.world = world;
        this.finished = new boolean[world.getChannels().size()];
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public boolean isFinished() {
        for (boolean b : finished) {
            if (!b) {
                return false;
            }
        }
        return true;
    }

    public boolean isFinished(int i) {
        return finished[i];
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public long getInterval() {
        return 1000 * 60 * 60 * 3;
    }

    @Override
    public void run() {
        setOpen(true);
        for (MapleChannel channel : world.getChannels()) {
            MapleMap eventMap = channel.getMap(MapId);
            eventMap.killAllMonsters();
            eventMap.clearDrops();

            final int PPIndex = Randomizer.nextInt(PortalPositions.length);
            int[] ipos = PortalPositions[PPIndex];
            Point pos = new Point(ipos[0], ipos[1]);
            MapleMonster monster = MapleLifeFactory.getMonster(MonsterId);

            if (monster != null) {
                MonsterListener DeathListener = new MonsterListener() {
                    @Override
                    public void monsterKilled(int aniTime) {
                        if (timeoutTask != null) {
                            timeoutTask.cancel();
                            timeoutTask = null;
                        }
                        finished[channel.getId() - 1] = true;
                        if (monster.getController() != null) {
                            // damaged -- because this is also invoked by MapleMap#killAllMonsters
                            channel.broadcastPacket(MaplePacketCreator.serverNotice(0, "The Space Slime has been defeated!"));
                        }
                    }
                };
                start = System.currentTimeMillis();
                monster.getListeners().add(DeathListener);
                finished[channel.getId() - 1] = false;
                eventMap.spawnMonsterOnGroudBelow(monster, pos);
                channel.broadcastPacket(MaplePacketCreator.serverNotice(0, "The Space Slime has spawned in the Outer Space, Planet Lucian"));
                LOGGER.info("spawned at {} in com.lucianms.server.events.channel {}", Arrays.toString(ipos), channel.getId());

                timeoutTask = TaskExecutor.createTask(new Runnable() {
                    @Override
                    public void run() {
                        eventMap.killMonster(monster.getId());
                        setOpen(false);
                    }
                }, 1000 * 60 * 5);
            } else {
                LOGGER.warn("Scheduled event 'Outer Space' was unable to spawn the monster " + MonsterId);
            }
        }
    }

    @Override
    public void end() {
        setOpen(false);
    }

    @Override
    public void registerPlayer(MapleCharacter player) {
        player.saveLocation("OTHER");
        player.changeMap(98);
        player.announce(MaplePacketCreator.showEffect("event/space/boss"));
        long endTime = start + (1000 * 60 * 5);
        long remainingTime = (long) ((endTime - System.currentTimeMillis()) / 1000d);
        if (remainingTime > 0) {
            player.announce(MaplePacketCreator.getClock((int) remainingTime));
        } else {
            LOGGER.info("Player '{}' entering Outer Space when it has ended", player.getName());
        }
    }

    @Override
    public void unregisterPlayer(MapleCharacter player) {
        if (player.getMapId() == MapId) {
            int returnMap = player.getSavedLocation("OTHER");
            if (returnMap == -1) {
                returnMap = 100000000;
                player.dropMessage("Your return map was obstructed");
            }
            player.changeMap(returnMap);
            player.clearSavedLocation(SavedLocationType.OTHER);
        }
    }
}
