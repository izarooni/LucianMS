package com.lucianms.features.emergency;

import com.lucianms.client.MapleCharacter;
import com.lucianms.events.ChangeMapEvent;
import com.lucianms.features.GenericEvent;
import com.lucianms.lang.annotation.PacketWorker;
import com.lucianms.scheduler.Task;
import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.server.maps.MapleMap;
import com.lucianms.server.world.MapleParty;
import com.lucianms.server.world.MaplePartyCharacter;
import tools.MaplePacketCreator;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author izarooni
 */
public abstract class Emergency extends GenericEvent {

    private class Timeout implements Runnable {

        private final boolean effect;

        Timeout(boolean effect) {
            this.effect = effect;
        }

        @Override
        public void run() {
            map.killAllMonsters();
            map.setRespawnEnabled(true);
            map.respawn();
            if (effect) {
                map.broadcastMessage(MaplePacketCreator.showEffect("dojang/timeOver"));
            }
            taskTimeout = TaskExecutor.cancelTask(taskTimeout);
        }
    }

    final ArrayList<MapleCharacter> players = new ArrayList<>();
    private MapleMap map = null;
    private Task taskTimeout = null;
    private boolean canceled = false;
    private int averageLevel;
    int delay = 120;

    Emergency(MapleCharacter player) {
    }

    @Override
    public boolean onPlayerChangeMapInternal(MapleCharacter player, MapleMap destination) {
        if (player.getParty() != null && player.getParty().getLeaderPlayerID() == player.getId()) {
            cancelTimeout();
            new Timeout(false).run();
            unregisterPlayers();
        } else {
            player.removeGenericEvent(this);
        }
        return true;
    }

    @PacketWorker
    public void onMapChange(ChangeMapEvent event) {
        onPlayerChangeMapInternal(event.getClient().getPlayer(), null);
    }

    final boolean registerPlayers(MapleCharacter player) {
        GenericEvent first = player.getGenericEvents().stream().filter(g -> g instanceof Emergency).findFirst().orElse(null);
        if (first != null) {
            canceled = true;
            logger().info("Player '{}' triggered but already in existing emergency instance", player.getName());
            return false;
        }
        player.addGenericEvent(this);
        players.add(player);
        int averageLevel;
        map = player.getMap();
        map.killAllMonsters();
        map.setRespawnEnabled(false);

        MapleParty party = player.getParty();
        Collection<MaplePartyCharacter> members;
        if (party != null && (members = party.values()).size() > 2) {
            averageLevel = 0;
            for (MaplePartyCharacter member : members) {
                averageLevel += member.getLevel();
            }
            averageLevel /= members.size();
        } else {
            averageLevel = player.getLevel();
        }
        this.averageLevel = averageLevel;
        taskTimeout = TaskExecutor.createTask(new Timeout(true), 1000 * delay);

        map.broadcastMessage(MaplePacketCreator.getClock(delay));
        return true;
    }

    final void unregisterPlayers() {
        players.forEach(p -> p.removeGenericEvent(this));
    }

    final void cancelTimeout() {
        taskTimeout = TaskExecutor.cancelTask(taskTimeout);
    }

    public MapleMap getMap() {
        return map;
    }

    public final boolean isCanceled() {
        return canceled;
    }

    final int getAverageLevel() {
        return averageLevel;
    }
}
