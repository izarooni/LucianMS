package server.events.custom.auto;

import client.MapleCharacter;
import net.server.world.World;
import provider.MapleDataProviderFactory;
import server.events.custom.GenericEvent;
import server.maps.MapleMap;
import server.maps.MapleMapFactory;
import tools.MaplePacketCreator;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author izarooni
 */
public abstract class GAutoEvent extends GenericEvent {

    private final World world;
    private static final int Channel_ID = 1;
    private MapleMapFactory mapleMapFactory = null;
    private Task respawnTask = null;

    // [f]irst time using this
    private ConcurrentHashMap<Integer, MapleCharacter> players = new ConcurrentHashMap<>();

    public GAutoEvent(World world, boolean nMapInstances) {
        this.world = world;
        if (nMapInstances) {
            mapleMapFactory = new MapleMapFactory(MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/Map.wz")), MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/String.wz")), world.getId(), Channel_ID);
        }
    }

    public final void broadcastWorldMessage(Object message) {
        if (message == null || message.toString().isEmpty()) {
            throw new IllegalArgumentException("Can't broadcast an empty messeage");
        }
        world.broadcastPacket(MaplePacketCreator.serverNotice(6, "[AutoEvent] " + message.toString()));
    }

    public final void loadMapInstance(int mapId, boolean skipMonsters) {
        if (mapleMapFactory == null) {
            throw new NullPointerException("Can't load map instances when auto event is set to not need them");
        }
        // haha slowly turning this factory into a builder FUCK
        mapleMapFactory.skipMonsters(skipMonsters).getMap(mapId);
    }

    public final MapleMap getMapInstance(int mapId) {
        if (mapleMapFactory == null) {
            throw new NullPointerException("Can't load map instances when auto event is set to not need them");
        }
        return mapleMapFactory.getMap(mapId);
    }

    public final Task registerRespawnTimer() {
        return respawnTask = createRepeatingTask(() -> mapleMapFactory.getMaps().values().forEach(MapleMap::respawn), 1000, 1000);
    }

    public final Task getRespawnTask() {
        return respawnTask;
    }

    public final World getWorld() {
        return world;
    }

    public final Collection<MapleCharacter> getPlayers() {
        List<MapleCharacter> ret = new ArrayList<>();
        ret.addAll(players.values());
        return Collections.unmodifiableList(ret);
    }

    public final int countPlayers() {
        return players.size();
    }

    public final boolean isPlayerRegistered(MapleCharacter player) {
        return players.containsKey(player.getId());
    }

    public final void registerPlayer(MapleCharacter player) {
        players.putIfAbsent(player.getId(), player);
        playerRegistered(player);
    }

    public final void unregisterPlayer(MapleCharacter player) {
        players.remove(player.getId());
        playerUnregistered(player);
    }

    public abstract void start();

    public abstract void stop();

    public abstract void playerRegistered(MapleCharacter player);

    public abstract void playerUnregistered(MapleCharacter player);
}
