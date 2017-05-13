package server.events.custom.auto;

import net.server.world.World;
import provider.MapleDataProviderFactory;
import server.events.custom.GenericEvent;
import server.maps.MapleMap;
import server.maps.MapleMapFactory;
import tools.MaplePacketCreator;

import java.io.File;

/**
 * @author izarooni
 */
public abstract class GenericAutoEvent extends GenericEvent {

    private final World world;
    private static final int Channel_ID = 1;
    private MapleMapFactory mapleMapFactory = null;
    private Task respawnTask = null;

    public GenericAutoEvent(World world, boolean nMapInstances) {
        this.world = world;
        if (nMapInstances) {
            mapleMapFactory = new MapleMapFactory(
                    MapleDataProviderFactory.getDataProvider(
                            new File(System.getProperty("wzpath") + "/Map.wz")),
                    MapleDataProviderFactory.getDataProvider(
                            new File(System.getProperty("wzpath") + "/String.wz")),
                    world.getId(), Channel_ID);
        }
    }

    public final void broadcastWorldMessage(Object message) {
        if (message == null || message.toString().isEmpty()) {
            throw new IllegalArgumentException("Can't broadcast an empty messeage");
        }
        world.broadcastPacket(MaplePacketCreator.serverNotice(6, message.toString()));
    }

    public final MapleMap getMapInstance(int mapId) {
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

    public abstract void start();

    public abstract void stop();
}
