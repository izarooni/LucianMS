package com.lucianms.features.auto;

import client.MapleCharacter;
import com.lucianms.features.GenericEvent;
import com.lucianms.scheduler.Task;
import com.lucianms.scheduler.TaskExecutor;
import net.server.world.World;
import server.FieldBuilder;
import server.maps.MapleMap;
import tools.MaplePacketCreator;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents the root class of a mini-game type auto event that is picked randomly upon several other possible like-wise events
 *
 * @author izarooni
 */
public abstract class GAutoEvent extends GenericEvent {

    private static final int ChannelID = 1;

    private final World world;
    private HashMap<Integer, MapleMap> maps = null;
    private Task respawnTask = null;

    // [f]irst time using this
    private ConcurrentHashMap<Integer, MapleCharacter> players = new ConcurrentHashMap<>();

    public GAutoEvent(World world, boolean nMapInstances) {
        this.world = world;
        if (nMapInstances) {
            maps = new HashMap<>(10);
        }
    }

    public final void broadcastWorldMessage(Object message) {
        if (message == null || message.toString().isEmpty()) {
            throw new IllegalArgumentException("Can't broadcast an empty messeage");
        }
        world.broadcastPacket(MaplePacketCreator.serverNotice(6, "[AutoEvent] " + message.toString()));
    }

    public final void loadMapInstance(int mapId, boolean skipMonsters) {
        if (maps == null) {
            throw new NullPointerException("Can't load map instances when auto event is set to not need them");
        }
        // haha slowly turning this factory into a builder FUCK
        FieldBuilder builder = new FieldBuilder(0, ChannelID, mapId).loadAll(); // toggle; load everything
        if (skipMonsters) {
            builder.loadMonsters(); // toggle; inverts the previous toggle thus becoming false
        }
        maps.put(mapId, builder.build());
    }

    public final MapleMap getMapInstance(int mapId) {
        if (maps == null) {
            throw new NullPointerException("Can't load map instances when auto event is set to not need them");
        }
        return maps.getOrDefault(mapId, new FieldBuilder(0, ChannelID, mapId).loadAll().build());
    }

    @Deprecated
    public final Task registerRespawnTimer() {
        return respawnTask = TaskExecutor.createRepeatingTask(() -> maps.values().forEach(MapleMap::respawn), 1000, 1000);
    }

    public final Task getRespawnTask() {
        return respawnTask;
    }

    public final World getWorld() {
        return world;
    }

    public final Collection<MapleCharacter> getPlayers() {
        List<MapleCharacter> ret = new ArrayList<>(players.values());
        return Collections.unmodifiableList(ret);
    }

    public final int countPlayers() {
        return players.size();
    }

    public final boolean isPlayerRegistered(MapleCharacter player) {
        return players.containsKey(player.getId());
    }

    @Override
    public final void registerPlayer(MapleCharacter player) {
        players.putIfAbsent(player.getId(), player);
        playerRegistered(player);
    }

    @Override
    public final void unregisterPlayer(MapleCharacter player) {
        players.remove(player.getId());
        playerUnregistered(player);
    }

    public abstract void start();

    public abstract void stop();

    public abstract void playerRegistered(MapleCharacter player);

    public abstract void playerUnregistered(MapleCharacter player);
}
