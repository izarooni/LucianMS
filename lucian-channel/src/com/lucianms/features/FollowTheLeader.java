package com.lucianms.features;

import com.lucianms.client.MapleCharacter;
import com.lucianms.events.PlayerMoveEvent;
import com.lucianms.lang.annotation.PacketWorker;
import com.lucianms.server.maps.MapleMap;

import java.util.Collection;

/**
 * @author izarooni
 */
public class FollowTheLeader extends GenericEvent {

    private final MapleMap map;
    private final MapleCharacter leader;

    private int playerCount = 0;

    public FollowTheLeader(MapleCharacter leader) {
        this.leader = leader;

        map = leader.getMap();

        Collection<MapleCharacter> players = leader.getMap().getCharacters();
        players.forEach(this::registerPlayer);

        registerAnnotationPacketEvents(this);
    }

    @Override
    public void dispose() {
        Collection<MapleCharacter> players = map.getPlayers();
        for (MapleCharacter player : players) {
            unregisterPlayer(player);
        }
        players.clear();
    }

    @Override
    public void registerPlayer(MapleCharacter player) {
        player.addGenericEvent(this);
        playerCount++;
    }

    @Override
    public void unregisterPlayer(MapleCharacter player) {
        if (player.removeGenericEvent(this)) {
            playerCount--;
            player.sendMessage(5, "You are not longer participating in Follow the Weenie");
            if (playerCount == 0 || player.getId() == leader.getId()) {
                dispose();
            }
        }
    }

    @Override
    public void onPlayerDisconnect(MapleCharacter player) {
        unregisterPlayer(player);
    }

    @Override
    public boolean onPlayerChangeMapInternal(MapleCharacter player, MapleMap destination) {
        unregisterPlayer(player);
        return true;
    }

    @PacketWorker
    public void onPlayerMoved(PlayerMoveEvent event) {
        MapleCharacter player = event.getClient().getPlayer();

        Collection<MapleCharacter> targets = player.getMap().getPlayers();
        for (MapleCharacter target : targets) {
            if (target.getId() != leader.getId() && target.getGenericEvents().contains(this)) {
                double distance = Math.abs(target.getPosition().distance(leader.getPosition()));
                if (distance > 200) {
                    unregisterPlayer(target);
                    target.setHpMp(0);
                    map.sendMessage(5, "{} has strayed too far from {}", target.getName(), leader.getName());
                }
            }
        }
        targets.clear();
    }
}
