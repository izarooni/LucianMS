package server.events.custom.auto;

import client.MapleCharacter;
import net.server.channel.handlers.MovePlayerHandler;
import net.server.world.World;
import server.movement.LifeMovement;
import server.movement.LifeMovementFragment;
import tools.annotation.PacketWorker;

public class AFlappyBird extends GAutoEvent {

    public AFlappyBird(World world) {
        super(world, true);
        registerAnnotationPacketEvents(this);
    }

    @Override
    public void start() {
        broadcastWorldMessage("Flappy Bird will being momentarily");
    }

    @Override
    public void stop() {
    }

    @PacketWorker
    public void onPlayerMove(MovePlayerHandler event) {
        MapleCharacter player = event.getClient().getPlayer();
        for (LifeMovementFragment frags : event.getMovements()) {
            if (frags instanceof LifeMovement) {
                LifeMovement mov = (LifeMovement) frags;
                if (mov.getType() !=  17) { // unsure
                    player.dropMessage("You are being kicked from the event for cheating");
                    unregisterPlayer(player);
                    break;
                }
            }
        }
    }

    @Override
    public void playerRegistered(MapleCharacter player) {
        player.dropMessage("Welcome to Flappy Bird!");
        player.addGenericEvent(this);
    }

    @Override
    public void playerUnregistered(MapleCharacter player) {
        player.removeGenericEvent(this);
    }
}
