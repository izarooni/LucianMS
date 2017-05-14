package server.events.custom.auto;

import client.MapleCharacter;
import net.server.world.World;

/**
 * @author izarooni
 */
public class AWhispyWoods extends GAutoEvent {

    public AWhispyWoods(World world) {
        super(world, true);
    }

    @Override
    public void start() {
        broadcastWorldMessage("Whispy Woods will begin momentarily");
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
