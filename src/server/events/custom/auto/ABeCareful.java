package server.events.custom.auto;

import client.MapleCharacter;
import net.server.world.World;

/**
 * @author izarooni
 */
public class ABeCareful extends GAutoEvent {

    public ABeCareful(World world) {
        super(world, true);
    }

    @Override
    public void start() {
        System.out.println("Be Careful! will begin momentarily");
    }

    @Override
    public void stop() {
    }

    @Override
    public void playerRegistered(MapleCharacter player) {

    }

    @Override
    public void playerUnregistered(MapleCharacter player) {

    }
}
