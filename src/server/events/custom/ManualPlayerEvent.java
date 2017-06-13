package server.events.custom;

import client.MapleCharacter;
import discord.Discord;
import net.server.Server;
import net.server.channel.Channel;
import server.maps.MapleMap;
import sx.blah.discord.util.MessageBuilder;
import tools.MaplePacketCreator;

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
    private Channel channel;
    private String name = null;
    private Point spawnPoint = null;
    private MapleMap map = null;
    private int gateTime = 60;
    private boolean open = false;

    public final HashMap<Integer, Participant> participants = new HashMap<>();
    private HashMap<String, Integer> winners = new HashMap<>();

    private int gateTask = 0;

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
        Server.getInstance().getWorld(getChannel().getWorld()).broadcastPacket(MaplePacketCreator.serverNotice(6, "[Event] " + message));
    }

    /**
     * Open event gates to allow players to join and automatically close once time reaches 0
     *
     * @param from a number to start counting down from
     * @param c    numbers to announce
     */
    public int openGates(final int from, int... c) {
        return (gateTask = createTask(new Runnable() {
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
        }, 1000).getId());
    }

    public void garbage() {
        channel = null;
        if (gateTask > 0) {
            cancelTask(gateTask);
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
            cancelTask(gateTask);
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

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
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
