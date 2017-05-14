package server.events.custom.auto;

import net.server.world.World;

public class AHeartlessWall extends GAutoEvent {

    public AHeartlessWall(World world) {
        super(world, true);
    }

    @Override
    public void start() {
        broadcastWorldMessage("Heartless Wall will begin momentarily");
    }

    @Override
    public void stop() {
    }
}
