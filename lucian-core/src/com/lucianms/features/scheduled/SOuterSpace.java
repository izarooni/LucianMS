package com.lucianms.features.scheduled;

import com.lucianms.client.MapleCharacter;
import com.lucianms.constants.ServerConstants;
import com.lucianms.scheduler.Task;
import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.server.channel.MapleChannel;
import com.lucianms.server.life.MapleLifeFactory;
import com.lucianms.server.life.MapleMonster;
import com.lucianms.server.life.MapleMonsterStats;
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
    private boolean[] finished;
    private int[] parties;
    private Task timeoutTask = null;
    private long start;
    private boolean open = false;

    public SOuterSpace(MapleWorld world) {
        this.world = world;
        this.finished = new boolean[world.getChannels().size()];
        this.parties = new int[world.getChannels().size()];
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

    /**
     * Used in NPC script 2040043
     *
     * @param i the channel ID of the player
     * @return true if the slime has been killed in the specified channel
     */
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
        finished = new boolean[world.getChannels().size()];
        parties = new int[world.getChannels().size()];

        for (MapleChannel channel : world.getChannels()) {
            final int channelID = channel.getId();
            final MapleMap eventMap = channel.getMap(MapId);
            MapleMonster slime = eventMap.getMonsterById(MonsterId);
            if (slime != null) {
                eventMap.killMonster(slime, null, false);
            }

            // spawn at a random set position in the map
            final int PPIndex = Randomizer.nextInt(PortalPositions.length);
            int[] ipos = PortalPositions[PPIndex];
            Point pos = new Point(ipos[0], ipos[1]);
            MapleMonster monster = MapleLifeFactory.getMonster(MonsterId);

            if (monster != null) {
                MonsterListener DeathListener = new MonsterListener() {
                    @Override
                    public void monsterKilled(MapleCharacter player, int aniTime) {
                        if (timeoutTask != null) {
                            timeoutTask.cancel();
                            timeoutTask = null;
                        }
                        finished[channelID - 1] = true;
                        if (monster.getController() != null) {
                            // damaged -- because this is also invoked by MapleMap#killAllMonsters
                            channel.broadcastPacket(MaplePacketCreator.serverNotice(0, "The channel " + channelID + " Space Slime has been defeated!"));
                        }
                        eventMap.broadcastMessage(MaplePacketCreator.getClock(30));
                        TaskExecutor.createTask(new Runnable() {
                            @Override
                            public void run() {
                                eventMap.warpEveryone(ServerConstants.HOME_MAP);
                            }
                        }, (1000 * 30));
                    }
                };

                // anders didn't remove the removeAfter property from the monster
                MapleMonsterStats stats = new MapleMonsterStats(monster.getStats());
                stats.setRemoveAfter(0);
                monster.setOverrideStats(stats);
                monster.getListeners().add(DeathListener);

                eventMap.spawnMonsterOnGroudBelow(monster, pos);
                start = System.currentTimeMillis();
                channel.broadcastPacket(MaplePacketCreator.serverNotice(0, "The Space Slime has spawned in the Outer Space, Planet Lucian"));
                LOGGER.info("spawned at {} in channel {}", Arrays.toString(ipos), channelID);

                timeoutTask = TaskExecutor.createTask(new Runnable() {
                    @Override
                    public void run() {
                        eventMap.warpEveryone(ServerConstants.HOME_MAP);
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
        MapleChannel channelServer = player.getClient().getChannelServer();
        int channelID = channelServer.getId();
        int reserve = (player.getParty() == null) ? player.getId() : player.getParty().getId();
        if (parties[channelID] == reserve || parties[channelID] == 0) {
            parties[channelID] = reserve;
            long endTime = start + (1000 * 60 * 5);
            long remainingTime = (long) ((endTime - System.currentTimeMillis()) / 1000d);
            if (remainingTime > 0) {
                player.saveLocation("OTHER");
                player.changeMap(98);
                player.announce(MaplePacketCreator.showEffect("event/space/boss"));
                player.announce(MaplePacketCreator.getClock((int) remainingTime));
                player.addGenericEvent(this);
            } else {
                LOGGER.info("Player '{}' entering Outer Space when it has ended", player.getName());
            }
        } else {
            player.sendMessage(5, "Another party is already fighting the Space Slime in this channel");
        }
    }

    @Override
    public void unregisterPlayer(MapleCharacter player) {
        player.removeGenericEvent(this);

        int channelID = player.getClient().getChannel();
        parties[channelID] = 0;

        int returnMap = player.getSavedLocation("OTHER");
        if (returnMap == -1) {
            returnMap = 100000000;
            player.dropMessage("Your return map was obstructed");
        }
        player.changeMap(returnMap);
        player.clearSavedLocation(SavedLocationType.OTHER);
    }

    @Override
    public boolean onPlayerChangeMapInternal(MapleCharacter player, MapleMap destination) {
        if (destination.getId() != MapId) {
            unregisterPlayer(player);
        }
        return false;
    }
}
