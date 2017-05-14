package server.events.custom.auto;

import net.server.world.World;

public class AFlappyBird extends GAutoEvent {

    public AFlappyBird(World world) {
        super(world, true);
    }

    @Override
    public void start() {
        broadcastWorldMessage("Flappy Bird will being momentarily");
    }

    @Override
    public void stop() {
    }
}
