package com.lucianms.features.auto;

import com.lucianms.client.MapleCharacter;
import com.lucianms.events.PlayerMoveEvent;
import com.lucianms.lang.annotation.PacketWorker;
import com.lucianms.server.movement.AbsoluteLifeMovement;
import com.lucianms.server.movement.LifeMovementFragment;
import com.lucianms.server.world.MapleWorld;

import java.util.HashMap;

/**
 * <p>
 * The goal of this event is to reach the (right side) end of the map without touching any platforms
 * </p>
 *
 * @author izarooni
 */
public class AFlappyBird extends GAutoEvent {

    private static final int EventMap = 21;

    private HashMap<Integer, Integer> returnMaps = new HashMap<>();

    public AFlappyBird(MapleWorld world) {
        super(world, true);
        registerAnnotationPacketEvents(this);
    }

    @Override
    public void start() {
        broadcastWorldMessage("Flappy Bird will begin momentarily");
        getMapInstance(EventMap); // pre-load map
    }

    @Override
    public void stop() {
        GAutoEventManager.setCurrentEvent(null);
    }

    @PacketWorker
    public void onPlayerMove(PlayerMoveEvent event) {
        // attempting to check player movement action, stance or position
        MapleCharacter player = event.getClient().getPlayer();
        for (LifeMovementFragment frags : event.getMovements()) {
            if (frags instanceof AbsoluteLifeMovement) {
                AbsoluteLifeMovement mov = (AbsoluteLifeMovement) frags;
                //                System.out.println(String.format("[%s] {unk=%d, player=%s}", getClass().getSimpleName(), mov.getUnk(), player.getName()));
                if (mov.getUnk() != 0) { // unsure
                    player.dropMessage("You are being kicked from the event for cheating");
                    unregisterPlayer(player);
                    break;
                }
            }
        }
    }

    @Override
    public void playerRegistered(MapleCharacter player) {
        if (player.addGenericEvent(this)) {
            returnMaps.put(player.getId(), player.getMapId());
            player.dropMessage("Welcome to Flappy Bird!");
            player.changeMap(getMapInstance(EventMap));
        }
    }

    @Override
    public void playerUnregistered(MapleCharacter player) {
        player.removeGenericEvent(this);
        if (returnMaps.containsKey(player.getId())) {
            int returnMap = returnMaps.remove(player.getId());
            player.changeMap(returnMap);
        }
    }
}
