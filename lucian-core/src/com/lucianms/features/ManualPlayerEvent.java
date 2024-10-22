package com.lucianms.features;

import com.lucianms.client.MapleCharacter;
import com.lucianms.scheduler.Task;
import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.server.channel.MapleChannel;
import com.lucianms.server.maps.MapleMap;

import java.awt.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author izarooni
 */
public class ManualPlayerEvent extends GenericEvent {

    public static final class Participant {
        public final int id;
        public final int returnMapId;

        public Participant(int id, int returnMapId) {
            this.id = id;
            this.returnMapId = returnMapId;
        }
    }

    private final MapleCharacter host;
    private MapleChannel channel;
    private String name = null;
    private Point spawnPoint = null;
    private MapleMap map = null;
    private int gateTime = 60;
    private boolean open = false;

    public final HashMap<Integer, Participant> participants = new HashMap<>();
    private HashMap<String, Integer> winners = new HashMap<>();

    private Task gateTask;

    public ManualPlayerEvent(MapleCharacter host) {
        this.host = host;
    }

    @Override
    public void registerPlayer(MapleCharacter player) {
    }

    @Override
    public void unregisterPlayer(MapleCharacter player) {
    }

    public void broadcastMessage(String message) {
        getChannel().getWorldServer().sendMessage(6, "[Event] {}", message);
    }

    /**
     * Open event gates to allow players to join and automatically close once time reaches 0
     *
     * @param from a number to start counting down from
     * @param c    numbers to announce
     */
    public Task openGates(final int from, int... c) {
        setOpen(true);
        return gateTask = TaskExecutor.createTask(new Runnable() {
            @Override
            public void run() {
                if (from == 0) {
                    setOpen(false);
                    broadcastMessage("The gates are now closed");
                    return;
                }
                for (int i : c) {
                    if (from == i) {
                        broadcastMessage(String.format("%d seconds left to join %s", from, (name == null ? "the event" : name)));
                        break;
                    }
                }
                gateTask = openGates(from - 1, c);
            }
        }, 1000);
    }

    public void garbage() {
        channel = null;
        if (gateTask != null) {
            gateTask = TaskExecutor.cancelTask(gateTask);
        }
    }

    public MapleMap getMap() {
        return map;
    }

    public void setMap(MapleMap map) {
        this.map = map;
        if (spawnPoint == null) {
            // default spawn point in case one is never set
            spawnPoint = map.getPortal(0).getPosition();
        }
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
        if (!this.open) {
            TaskExecutor.cancelTask(gateTask);
        }
    }

    public void addWinners(String... usernames) {
        for (String username : usernames) {
            int i = winners.getOrDefault(username, 0);
            winners.put(username, i + 1);
        }
    }

    public void removeWinners(String... usernames) {
        for (String username : usernames) {
            winners.remove(username);
        }
    }

    public Map<String, Integer> getWinners() {
        return Collections.unmodifiableMap(winners);
    }

    public MapleCharacter getHost() {
        return host;
    }

    public MapleChannel getChannel() {
        return channel;
    }

    public void setChannel(MapleChannel channel) {
        this.channel = channel;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Point getSpawnPoint() {
        return spawnPoint;
    }

    public void setSpawnPoint(Point spawnPoint) {
        this.spawnPoint = spawnPoint;
    }

    public int getGateTime() {
        return gateTime;
    }

    public void setGateTime(int gateTime) {
        this.gateTime = gateTime;
    }
}
