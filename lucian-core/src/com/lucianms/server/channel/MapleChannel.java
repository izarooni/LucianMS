package com.lucianms.server.channel;

import com.lucianms.client.MapleCharacter;
import com.lucianms.events.gm.MapleEvent;
import com.lucianms.io.scripting.event.EventScriptManager;
import com.lucianms.nio.server.MapleServerInboundHandler;
import com.lucianms.server.ConcurrentMapStorage;
import com.lucianms.server.FieldBuilder;
import com.lucianms.server.Server;
import com.lucianms.server.expeditions.MapleExpedition;
import com.lucianms.server.life.FakePlayer;
import com.lucianms.server.maps.HiredMerchant;
import com.lucianms.server.maps.MapleMap;
import com.lucianms.server.world.MapleWorld;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.PacketAnnouncer;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

public final class MapleChannel implements PacketAnnouncer {

    private static final Logger LOGGER = LoggerFactory.getLogger(MapleChannel.class);

    private final int world, channel;
    private final int port;
    private final InetAddress networkAddress;
    private MapleServerInboundHandler serverHandler;
    private ArrayList<MapleExpedition> expeditions = new ArrayList<>();
    private ConcurrentMapStorage<Integer, MapleMap> maps = new ConcurrentMapStorage<>();
    private HashMap<Integer, HiredMerchant> hiredMerchants = new HashMap<>();
    private HashMap<Integer, Integer> storedVars = new HashMap<>();
    private EventScriptManager eventScriptManager;
    private MapleEvent event;

    private ReentrantReadWriteLock merchant_lock = new ReentrantReadWriteLock(true);

    public MapleChannel(final int world, final int channel) throws UnknownHostException {
        this.world = world;
        this.channel = channel;
        this.port = (7575 + (this.channel - 1)) + (world * 100);
        networkAddress = InetAddress.getByName(Server.getConfig().getString("ChannelServer"));
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

    public Collection<MapleMap> getMaps() {
        return maps.values();
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
            Collection<MapleCharacter> chars = fOld.getPlayers(p -> !(p instanceof FakePlayer));
            for (MapleCharacter player : chars) {
                player.changeMap(fNew);
            }
            chars.clear();
            maps.put(mapID, fNew);
        }
    }

    public int getWorld() {
        return world;
    }

    public MapleWorld getWorldServer() {
        return Server.getWorld(getWorld());
    }

    @Override
    public Collection<MapleCharacter> getPlayers() {
        return getWorldServer().getPlayers(p -> p.getClient().getChannel() == getId());
    }

    public int getUserCount() {
        Collection<MapleCharacter> players = getPlayers();
        int sum = players.stream().mapToInt(p -> 1).sum();
        players.clear();
        return sum;
    }

    public final int getId() {
        return channel;
    }

    public InetAddress getNetworkAddress() {
        return networkAddress;
    }

    public int getPort() {
        return port;
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
            MapleCharacter chr = getWorldServer().getPlayerStorage().get(characterId);
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