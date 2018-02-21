/*
This file is part of the OdinMS Maple Story Server
Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
Matthias Butz <matze@odinms.de>
Jan Christian Meyer <vimes@odinms.de>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation version 3 as published by
the Free Software Foundation. You may not use, modify or distribute
this program under any other version of the GNU Affero General Public
License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.server.channel;

import client.MapleCharacter;
import net.MapleServerHandler;
import net.mina.MapleCodecFactory;
import net.server.PlayerStorage;
import net.server.Server;
import net.server.world.MapleParty;
import net.server.world.MaplePartyCharacter;
import org.apache.commons.io.FilenameUtils;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.buffer.SimpleBufferAllocator;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.SocketSessionConfig;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scheduler.TaskExecutor;
import scripting.event.EventScriptManager;
import server.events.gm.MapleEvent;
import server.expeditions.MapleExpedition;
import server.maps.HiredMerchant;
import server.maps.MapleMap;
import server.maps.MapleMapFactory;
import server.partyquest.carnival.MCarnivalLobbyManager;
import tools.MaplePacketCreator;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

public final class Channel {

    private static final Logger LOGGER = LoggerFactory.getLogger(Channel.class);

    private PlayerStorage players = new PlayerStorage();
    private int world, channel;
    private IoAcceptor acceptor;
    private String ip, serverMessage;
    private MapleMapFactory mapFactory;
    private EventScriptManager eventScriptManager;
    private Map<Integer, HiredMerchant> hiredMerchants = new HashMap<>();
    private final Map<Integer, Integer> storedVars = new HashMap<>();
    private ReentrantReadWriteLock merchant_lock = new ReentrantReadWriteLock(true);
    private List<MapleExpedition> expeditions = new ArrayList<>();
    private MCarnivalLobbyManager carnivalLobbyManager = null;
    private MapleEvent event;

    public Channel(final int world, final int channel) {
        this.world = world;
        this.channel = channel;
        this.mapFactory = new MapleMapFactory(world, channel);
        TaskExecutor.createRepeatingTask(() -> mapFactory.getMaps().forEach(MapleMap::respawn), 10000, 10000);
        reloadEventScriptManager();
        carnivalLobbyManager = new MCarnivalLobbyManager(this);
        int port = 7575;
        try {
            port = 7575 + this.channel - 1;
            port += (world * 100);
            ip = Server.getInstance().getConfig().getString("ServerHost") + ":" + port;

            IoBuffer.setUseDirectBuffer(false);
            IoBuffer.setAllocator(new SimpleBufferAllocator());

            acceptor = new NioSocketAcceptor();
            acceptor.setHandler(new MapleServerHandler(world, channel));
            acceptor.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, 30);
            ((SocketSessionConfig) acceptor.getSessionConfig()).setTcpNoDelay(true);
            acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(new MapleCodecFactory()));
            acceptor.bind(new InetSocketAddress(port));

        } catch (Exception e) {
            e.printStackTrace();
        }
        LOGGER.info("Channel {} bind to port {}", getId(), port);
    }

    public MCarnivalLobbyManager getCarnivalLobbyManager() {
        return carnivalLobbyManager;
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
        this.eventScriptManager.init();
    }

    public final void shutdown() {
        try {
            closeAllMerchants();
            players.disconnectAll();

            acceptor.unbind();
            acceptor.dispose();
            acceptor = null;
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

    public MapleMapFactory getMapFactory() {
        return mapFactory;
    }

    public int getWorld() {
        return world;
    }

    public void addPlayer(MapleCharacter chr) {
        players.addPlayer(chr);
        chr.announce(MaplePacketCreator.serverMessage(serverMessage));
    }

    public PlayerStorage getPlayerStorage() {
        return players;
    }

    public void removePlayer(MapleCharacter chr) {
        players.removePlayer(chr.getId());
    }

    public int getConnectedClients() {
        return players.getAllCharacters().size();
    }

    public void broadcastPacket(final byte[] data) {
        for (MapleCharacter chr : players.getAllCharacters()) {
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
        for (MapleCharacter chr : players.getAllCharacters()) {
            if (chr.isGM()) {
                chr.announce(data);
            }
        }
    }

    public List<MapleCharacter> getPartyMembers(MapleParty party) {
        List<MapleCharacter> partym = new ArrayList<>(8);
        for (MaplePartyCharacter partychar : party.getMembers()) {
            if (partychar.getChannel() == getId()) {
                MapleCharacter chr = getPlayerStorage().getCharacterByName(partychar.getName());
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
        PlayerStorage playerStorage = getPlayerStorage();
        for (int characterId : characterIds) {
            MapleCharacter chr = playerStorage.getCharacterById(characterId);
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
        return getPlayerStorage().getCharacterByName(name) != null;
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