package com.lucianms.server.channel;

import com.lucianms.client.MapleCharacter;
import com.lucianms.events.gm.MapleEvent;
import com.lucianms.io.scripting.event.EventScriptManager;
import com.lucianms.nio.server.MapleServerInboundHandler;
import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.server.ConcurrentMapStorage;
import com.lucianms.server.FieldBuilder;
import com.lucianms.server.Server;
import com.lucianms.server.expeditions.MapleExpedition;
import com.lucianms.server.maps.HiredMerchant;
import com.lucianms.server.maps.MapleMap;
import com.lucianms.server.world.MapleParty;
import com.lucianms.server.world.MaplePartyCharacter;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.MaplePacketCreator;

import java.io.File;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

public final class MapleChannel {

    private static final Logger LOGGER = LoggerFactory.getLogger(MapleChannel.class);

    private MapleServerInboundHandler serverHandler;
    private int world, channel;
    private String ip, serverMessage;
    private ConcurrentMapStorage<Integer, MapleCharacter> players = new ConcurrentMapStorage<>();
    private ConcurrentMapStorage<Integer, MapleMap> maps = new ConcurrentMapStorage<>();
    private EventScriptManager eventScriptManager;
    private Map<Integer, HiredMerchant> hiredMerchants = new HashMap<>();
    private final Map<Integer, Integer> storedVars = new HashMap<>();
    private ReentrantReadWriteLock merchant_lock = new ReentrantReadWriteLock(true);
    private List<MapleExpedition> expeditions = new ArrayList<>();
    private MapleEvent event;

    public MapleChannel(final int world, final int channel) {
        this.world = world;
        this.channel = channel;
        final int port = (7575 + (this.channel - 1)) + (world * 100);
        ip = Server.getConfig().getString("ServerHost") + ":" + port;

        if (Server.getRunningOperation() == Server.RunningOperation.Channel) {
            TaskExecutor.createRepeatingTask(() -> maps.forEach(MapleMap::respawn), 5000);
        }
    }

    public MapleServerInboundHandler getServerHandler() {
        return serverHandler;
    }

    public void setServerHandler(MapleServerInboundHandler serverHandler) {
        this.serverHandler = serverHandler;
    }

    public void reloadEventScriptManager() {
        if (eventScriptManager != null) {
            eventScriptManager.close();
        }
        eventScriptManager = new EventScriptManager(this);
        File eventFiles = new File("scripts/event");
        File[] files = eventFiles.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    String scriptName = FilenameUtils.removeExtension(file.getName());
                    eventScriptManager.putManager(scriptName);
                }
            }
        }
        eventScriptManager.init();
    }

    public final void shutdown() {
        try {
            if (eventScriptManager != null) {
                eventScriptManager.close();
            }
            getPlayerStorage().forEach(MapleCharacter::saveToDB);
            closeAllMerchants();
            LOGGER.info("Shut down world {} channel {}", world, channel);
        } catch (Exception e) {
            LOGGER.error("Exception while shutting down world {} channel {}", world, channel);
            e.printStackTrace();
        }
    }

    public void closeAllMerchants() {
        WriteLock wlock = merchant_lock.writeLock();
        wlock.lock();
        try {
            final Iterator<HiredMerchant> hmit = hiredMerchants.values().iterator();
            while (hmit.hasNext()) {
                hmit.next().forceClose();
                hmit.remove();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            wlock.unlock();
        }
    }

    public boolean isMapLoaded(int mapID) {
        return maps.get(mapID) != null;
    }

    public MapleMap getMap(int mapID) {
        return maps.computeIfAbsent(mapID, id -> new FieldBuilder(world, channel, mapID).loadAll().build());
    }

    public MapleMap removeMap(int mapID) {
        return maps.remove(mapID);
    }

    public void reloadMap(int mapID) {
        MapleMap fOld, fNew;
        if ((fOld = maps.remove(mapID)) != null) {
            fNew = getMap(mapID);
            Collection<MapleCharacter> chars = new ArrayList<>(fOld.getCharacters());
            try {
                for (MapleCharacter player : chars) {
                    player.changeMap(fNew);
                }
            } finally {
                chars.clear();
            }
            maps.put(mapID, fNew);
        }
    }

    public int getWorld() {
        return world;
    }

    public void addPlayer(MapleCharacter chr) {
        players.put(chr.getId(), chr);
        chr.announce(MaplePacketCreator.serverMessage(serverMessage));
    }

    public ConcurrentMapStorage<Integer, MapleCharacter> getPlayerStorage() {
        return players;
    }

    public void removePlayer(MapleCharacter chr) {
        players.remove(chr.getId());
    }

    public int getConnectedClients() {
        return players.values().size();
    }

    public void broadcastPacket(final byte[] data) {
        for (MapleCharacter chr : players.values()) {
            chr.announce(data);
        }
    }

    public final int getId() {
        return channel;
    }

    public String getIP() {
        return ip;
    }

    public MapleEvent getEvent() {
        return event;
    }

    public void setEvent(MapleEvent event) {
        this.event = event;
    }

    public EventScriptManager getEventScriptManager() {
        return eventScriptManager;
    }

    public void broadcastGMPacket(final byte[] data) {
        for (MapleCharacter chr : players.values()) {
            if (chr.isGM()) {
                chr.announce(data);
            }
        }
    }

    public List<MapleCharacter> getPartyMembers(MapleParty party) {
        List<MapleCharacter> partym = new ArrayList<>(8);
        for (MaplePartyCharacter partychar : party.getMembers()) {
            if (partychar.getChannel() == getId()) {
                MapleCharacter chr = getPlayerStorage().find(c -> c.getName().equalsIgnoreCase(partychar.getName()));
                if (chr != null) {
                    partym.add(chr);
                }
            }
        }
        return partym;
    }

    public Map<Integer, HiredMerchant> getHiredMerchants() {
        return hiredMerchants;
    }

    public void addHiredMerchant(int chrid, HiredMerchant hm) {
        WriteLock wlock = merchant_lock.writeLock();
        wlock.lock();
        try {
            hiredMerchants.put(chrid, hm);
        } finally {
            wlock.unlock();
        }
    }

    public void removeHiredMerchant(int chrid) {
        WriteLock wlock = merchant_lock.writeLock();
        wlock.lock();
        try {
            hiredMerchants.remove(chrid);
        } finally {
            wlock.unlock();
        }
    }

    public int[] multiBuddyFind(int charIdFrom, int[] characterIds) {
        List<Integer> ret = new ArrayList<>(characterIds.length);
        for (int characterId : characterIds) {
            MapleCharacter chr = getPlayerStorage().get(characterId);
            if (chr != null) {
                if (chr.getBuddylist().containsVisible(charIdFrom)) {
                    ret.add(characterId);
                }
            }
        }
        int[] retArr = new int[ret.size()];
        int pos = 0;
        for (Integer i : ret) {
            retArr[pos++] = i;
        }
        return retArr;
    }

    public List<MapleExpedition> getExpeditions() {
        return expeditions;
    }

    public boolean isConnected(String name) {
        return getPlayerStorage().find(c -> c.getName().equalsIgnoreCase(name)) != null;
    }

    public void setServerMessage(String message) {
        this.serverMessage = message;
        broadcastPacket(MaplePacketCreator.serverMessage(message));
    }

    public int getStoredVar(int key) {
        if (storedVars.containsKey(key)) {
            return storedVars.get(key);
        }
        return 0;
    }

    public void setStoredVar(int key, int val) {
        this.storedVars.put(key, val);
    }
}