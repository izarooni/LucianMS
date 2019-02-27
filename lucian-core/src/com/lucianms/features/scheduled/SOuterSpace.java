package com.lucianms.features.scheduled;

import com.lucianms.client.MapleCharacter;
import com.lucianms.constants.ServerConstants;
import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.server.FieldBuilder;
import com.lucianms.server.life.MapleLifeFactory;
import com.lucianms.server.life.MapleMonster;
import com.lucianms.server.life.MapleMonsterStats;
import com.lucianms.server.life.MonsterListener;
import com.lucianms.server.maps.MapleMap;
import com.lucianms.server.world.MapleWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.MaplePacketCreator;
import tools.Randomizer;

import java.awt.*;
import java.util.HashMap;
import java.util.function.Function;

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

    private HashMap<Integer, MapleMap> mapInstances = new HashMap<>(25);
    private final MapleWorld world;
    private long start;
    private boolean open = false;

    public SOuterSpace(MapleWorld world) {
        this.world = world;
    }

    public MapleMap getMapInstance(MapleCharacter player) {
        int worldID = player.getClient().getWorld();
        int channelID = player.getClient().getChannel();
        int reserve = (player.getParty() == null) ? player.getId() : player.getParty().getId();
        return mapInstances.computeIfAbsent(reserve, new Function<Integer, MapleMap>() {
            @Override
            public MapleMap apply(Integer id) {
                MapleMap build = new FieldBuilder(worldID, channelID, MapId).loadAll().build();
                build.setInstanced(true);
                SOuterSpace.this.summonSlime(build);
                return build;
            }
        });
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    private void summonSlime(final MapleMap map) {
        // spawn at a random set position in the map
        final int PPIndex = Randomizer.nextInt(PortalPositions.length);
        int[] ipos = PortalPositions[PPIndex];
        Point pos = new Point(ipos[0], ipos[1]);
        MapleMonster monster = MapleLifeFactory.getMonster(MonsterId);

        if (monster != null) {
            // anders didn't remove the removeAfter property from the monster
            MapleMonsterStats stats = new MapleMonsterStats(monster.getStats());
            stats.setRemoveAfter(0);
            monster.setOverrideStats(stats);
            monster.getListeners().add(new SlimeKingHandler());

            map.spawnMonsterOnGroudBelow(monster, pos);
        } else {
            LOGGER.warn("Scheduled event 'Outer Space' was unable to spawn the monster " + MonsterId);
        }
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
        start = System.currentTimeMillis();
        mapInstances.clear();
        world.broadcastMessage(0, "The Space Slime has spawned in the Outer Space, Planet Lucian");
        TaskExecutor.createTask(this::end, 1000 * 60 * 5);
    }

    @Override
    public void end() {
        setOpen(false);
        for (MapleMap maps : mapInstances.values()) {
            maps.warpEveryone(ServerConstants.HOME_MAP);
        }
    }

    @Override
    public void registerPlayer(MapleCharacter player) {
        MapleMap destination = getMapInstance(player);
        long endTime = start + (1000 * 60 * 5);
        long remainingTime = (long) ((endTime - System.currentTimeMillis()) / 1000d);
        if (destination.isInstanced() && remainingTime > 0) {
            player.changeMap(destination);
            player.announce(MaplePacketCreator.showEffect("event/space/boss"));
            player.announce(MaplePacketCreator.getClock((int) remainingTime));
            player.addGenericEvent(this);
        } else {
            player.sendMessage(5, "Unfortunately the Space Slime has left Planet Lucian");
        }
    }

    @Override
    public void unregisterPlayer(MapleCharacter player) {
        player.removeGenericEvent(this);
    }

    @Override
    public boolean onPlayerChangeMapInternal(MapleCharacter player, MapleMap destination) {
        if (destination.getId() != MapId) {
            unregisterPlayer(player);
        }
        return true;
    }

    private class SlimeKingHandler extends MonsterListener {
        @Override
        public void monsterKilled(MapleMonster monster, MapleCharacter player) {
            MapleMap map = monster.getMap();

            map.setInstanced(false);
            map.broadcastMessage(MaplePacketCreator.getClock(60));
            TaskExecutor.createTask(() -> map.warpEveryone(ServerConstants.HOME_MAP), (1000 * 60));
        }
    }
}
