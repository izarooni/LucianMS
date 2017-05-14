package server.events.custom.auto;

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
}
