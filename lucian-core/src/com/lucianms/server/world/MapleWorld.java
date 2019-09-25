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
package com.lucianms.server.world;

import com.lucianms.client.BuddyList;
import com.lucianms.client.BuddyList.BuddyAddResult;
import com.lucianms.client.BuddyList.BuddyOperation;
import com.lucianms.client.BuddylistEntry;
import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleFamily;
import com.lucianms.features.ManualPlayerEvent;
import com.lucianms.features.scheduled.SAutoEvent;
import com.lucianms.lang.DuplicateEntryException;
import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.server.ConcurrentMapStorage;
import com.lucianms.server.Server;
import com.lucianms.server.channel.CharacterIdChannelPair;
import com.lucianms.server.channel.MapleChannel;
import com.lucianms.server.guild.MapleGuild;
import com.lucianms.server.guild.MapleGuildCharacter;
import com.lucianms.server.guild.MapleGuildSummary;
import com.lucianms.server.maps.MapleTvManager;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.Disposable;
import tools.MaplePacketCreator;
import tools.PacketAnnouncer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Predicate;

/**
 * @author kevintjuh93
 */
public class MapleWorld implements PacketAnnouncer, Disposable {

    private static final Logger LOGGER = LoggerFactory.getLogger(MapleWorld.class);

    private final ConcurrentMapStorage<Integer, MapleMessenger> messengers = new ConcurrentMapStorage<>(20);
    private final ConcurrentMapStorage<Integer, MapleCharacter> playerStorage;
    private HikariDataSource hikari;
    private int id, flag, exprate, droprate, mesorate, bossdroprate;
    private String eventmsg;
    private String serverMessage;
    private List<MapleChannel> channels = new ArrayList<>();
    private Map<Integer, MapleParty> parties = new HashMap<>();
    private Map<Integer, MapleGuildSummary> gsStore = new HashMap<>();
    private Map<Integer, MapleFamily> families = new LinkedHashMap<>();
    private Map<String, SAutoEvent> scheduledEvents = new HashMap<>();
    private MapleTvManager mapleTvManager;
    private ManualPlayerEvent playerEvent;
    private long lastPlayerEventTime;

    public MapleWorld(int world, int flag, String eventmsg, int exprate, int droprate, int mesorate, int bossdroprate) {
        this.id = world;
        this.flag = flag;
        this.eventmsg = eventmsg;
        this.exprate = exprate;
        this.droprate = droprate;
        this.mesorate = mesorate;
        this.bossdroprate = bossdroprate;

        playerStorage = new ConcurrentMapStorage<>(100);
    }

    public ConcurrentMapStorage<Integer, MapleCharacter> getPlayerStorage() {
        return playerStorage;
    }

    public HikariDataSource getHikari() {
        return hikari;
    }

    public void setHikari(HikariDataSource hikari) {
        this.hikari = hikari;
    }

    public Connection getConnection() throws SQLException {
        return hikari.getConnection();
    }

    public SAutoEvent getScheduledEvent(String name) {
        return scheduledEvents.get(name);
    }

    public void addScheduledEvent(SAutoEvent event) {
        if (scheduledEvents.containsKey(event.getName())) {
            throw new DuplicateEntryException(String.format("World generic event (%s) already exists with name '%s'", event.getClass().getSimpleName(), event.getName()));
        }
        scheduledEvents.put(event.getName(), event);
        TaskExecutor.createRepeatingTask(event::run, event.getInterval(), event.getInterval());
    }

    public Map<String, SAutoEvent> getScheduledEvents() {
        return scheduledEvents;
    }

    public List<MapleChannel> getChannels() {
        return channels;
    }

    public MapleChannel getChannel(int channel) {
        return channels.get(channel - 1);
    }

    public void addChannel(MapleChannel channel) {
        channels.add(channel);
    }

    public void removeChannel(int channel) {
        channels.remove(channel);
    }

    public ConcurrentMapStorage<Integer, MapleMessenger> getMessengers() {
        return messengers;
    }

    public int getFlag() {
        return flag;
    }

    public void setFlag(byte flag) {
        this.flag = flag;
    }

    public String getEventMessage() {
        return eventmsg;
    }

    public String getServerMessage() {
        return serverMessage;
    }

    public void setServerMessage(String serverMessage) {
        this.serverMessage = serverMessage;
        sendPacket(MaplePacketCreator.serverMessage(serverMessage));
    }

    public int getExpRate() {
        return exprate;
    }

    public void setExpRate(int exp) {
        this.exprate = exp;
    }

    public int getDropRate() {
        return droprate;
    }

    public void setDropRate(int drop) {
        this.droprate = drop;
    }

    public int getMesoRate() {
        return mesorate;
    }

    public void setMesoRate(int meso) {
        this.mesorate = meso;
    }

    public int getBossDropRate() {
        return bossdroprate;
    }

    public MapleCharacter findPlayer(Predicate<MapleCharacter> predicate) {
        return getPlayerStorage().find(predicate);
    }

    public Optional<MapleCharacter> tryGetPlayer(int playerID) {
        return Optional.ofNullable(playerStorage.get(playerID));
    }

    public int getId() {
        return id;
    }

    public void addFamily(int id, MapleFamily f) {
        families.putIfAbsent(id, f);
    }

    public MapleTvManager getMapleTvManager() {
        return mapleTvManager;
    }

    public void setMapleTvManager(MapleTvManager mapleTvManager) {
        this.mapleTvManager = mapleTvManager;
    }

    public ManualPlayerEvent getPlayerEvent() {
        return playerEvent;
    }

    public void setPlayerEvent(ManualPlayerEvent playerEvent) {
        this.playerEvent = playerEvent;
        this.lastPlayerEventTime = System.currentTimeMillis();
    }

    public long getLastPlayerEventTime() {
        return lastPlayerEventTime;
    }

    public MapleFamily getFamily(int id) {
        return families.get(id);
    }

    public MapleGuild getGuild(MapleGuildCharacter mgc) {
        int gid = mgc.getGuildId();
        MapleGuild g;
        g = Server.getGuild(gid, mgc.getWorld(), mgc);
        if (gsStore.get(gid) == null) {
            gsStore.put(gid, new MapleGuildSummary(g));
        }
        return g;
    }

    public MapleGuildSummary getGuildSummary(int gid, int wid) {
        if (gsStore.containsKey(gid)) {
            return gsStore.get(gid);
        } else {
            MapleGuild g = Server.getGuild(gid, wid, null);
            if (g != null) {
                gsStore.put(gid, new MapleGuildSummary(g));
            }
            return gsStore.get(gid);
        }
    }

    public void updateGuildSummary(int gid, MapleGuildSummary mgs) {
        gsStore.put(gid, mgs);
    }

    public void reloadGuildSummary() {
        MapleGuild g;
        for (int i : gsStore.keySet()) {
            g = Server.getGuild(i, getId(), null);
            if (g != null) {
                gsStore.put(i, new MapleGuildSummary(g));
            } else {
                gsStore.remove(i);
            }
        }
    }

    public void setGuildAndRank(List<Integer> cids, int guildid, int rank, int exception) {
        for (int cid : cids) {
            if (cid != exception) {
                setGuildAndRank(cid, guildid, rank);
            }
        }
    }

    public void setOfflineGuildStatus(int guildid, int guildrank, int cid) {
        try (Connection con = Server.getConnection(); PreparedStatement ps = con.prepareStatement("UPDATE characters SET guildid = ?, guildrank = ? WHERE id = ?")) {
            ps.setInt(1, guildid);
            ps.setInt(2, guildrank);
            ps.setInt(3, cid);
            ps.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setGuildAndRank(int cid, int guildid, int rank) {
        MapleCharacter mc = getPlayerStorage().get(cid);
        if (mc == null) {
            return;
        }
        boolean bDifferentGuild;
        if (guildid == -1 && rank == -1) {
            bDifferentGuild = true;
        } else {
            bDifferentGuild = guildid != mc.getGuildId();
            mc.setGuildId(guildid);
            mc.setGuildRank(rank);
            mc.saveGuildStatus();
        }
        if (bDifferentGuild) {
            mc.getMap().sendPacketExclude(MaplePacketCreator.removePlayerFromMap(cid), mc);
            mc.getMap().sendPacketExclude(MaplePacketCreator.getUserEnterField(mc), mc);
        }
    }

    public void changeEmblem(int gid, List<Integer> affectedPlayers, MapleGuildSummary mgs) {
        updateGuildSummary(gid, mgs);
        sendPacket(affectedPlayers, MaplePacketCreator.guildEmblemChange(gid, mgs.getLogoBG(), mgs.getLogoBGColor(), mgs.getLogo(), mgs.getLogoColor()), -1);
        setGuildAndRank(affectedPlayers, -1, -1, -1);    //respawn player
    }

    public void sendPacket(List<Integer> targetIds, final byte[] packet, int exception) {
        MapleCharacter c;
        for (int i : targetIds) {
            if (i == exception) {
                continue;
            }
            c = getPlayerStorage().get(i);
            if (c != null) {
                c.getClient().announce(packet);
            }
        }
    }

    public Map<Integer, MapleParty> getParties() {
        return parties;
    }

    public MapleParty getParty(int partyid) {
        return parties.get(partyid);
    }

    public int find(String name) {
        int channel = -1;
        MapleCharacter chr = findPlayer(p -> p.getName().equalsIgnoreCase(name));
        if (chr != null) {
            channel = chr.getClient().getChannel();
        }
        return channel;
    }

    public int find(int id) {
        int channel = -1;
        MapleCharacter chr = getPlayerStorage().get(id);
        if (chr != null) {
            channel = chr.getClient().getChannel();
        }
        return channel;
    }

    public void buddyChat(int[] recipientCharacterIds, int cidFrom, String nameFrom, String chattext) {
        for (int characterId : recipientCharacterIds) {
            MapleCharacter chr = getPlayerStorage().get(characterId);
            if (chr != null) {
                if (chr.getBuddylist().containsVisible(cidFrom)) {
                    chr.getClient().announce(MaplePacketCreator.multiChat(nameFrom, chattext, 0));
                }
            }
        }
    }

    public CharacterIdChannelPair[] multiBuddyFind(int charIdFrom, int[] characterIds) {
        List<CharacterIdChannelPair> foundsChars = new ArrayList<>(characterIds.length);
        for (MapleChannel ch : getChannels()) {
            for (int charid : ch.multiBuddyFind(charIdFrom, characterIds)) {
                foundsChars.add(new CharacterIdChannelPair(charid, ch.getId()));
            }
        }
        return foundsChars.toArray(new CharacterIdChannelPair[foundsChars.size()]);
    }

    public MapleMessenger getMessenger(int messengerid) {
        return messengers.get(messengerid);
    }

    public boolean isUserOnline(String charName) {
        return findPlayer(p -> p.getName().equalsIgnoreCase(charName)) != null;
    }

    public void whisper(String sender, String target, int channel, String message) {
        if (isUserOnline(target)) {
            findPlayer(p -> p.getName().equalsIgnoreCase(target)).getClient().announce(MaplePacketCreator.getWhisper(sender, channel, message));
        }
    }

    public BuddyAddResult requestBuddyAdd(String addName, int channelFrom, int cidFrom, String nameFrom) {
        MapleCharacter addChar = findPlayer(p -> p.getName().equalsIgnoreCase(addName));
        if (addChar != null) {
            BuddyList buddylist = addChar.getBuddylist();
            if (buddylist.isFull()) {
                return BuddyAddResult.BUDDYLIST_FULL;
            }
            if (!buddylist.contains(cidFrom)) {
                buddylist.addBuddyRequest(addChar.getClient(), cidFrom, nameFrom, channelFrom);
            } else if (buddylist.containsVisible(cidFrom)) {
                return BuddyAddResult.ALREADY_ON_LIST;
            }
        }
        return BuddyAddResult.OK;
    }

    public void buddyChanged(int cid, int cidFrom, String name, int channel, BuddyOperation operation) {
        MapleCharacter addChar = getPlayerStorage().get(cid);
        if (addChar != null) {
            BuddyList buddylist = addChar.getBuddylist();
            switch (operation) {
                case ADDED:
                    if (buddylist.contains(cidFrom)) {
                        buddylist.put(new BuddylistEntry(name, "Default Group", cidFrom, channel, true));
                        addChar.getClient().announce(MaplePacketCreator.updateBuddyChannel(cidFrom, (byte) (channel - 1)));
                    }
                    break;
                case DELETED:
                    if (buddylist.contains(cidFrom)) {
                        buddylist.put(new BuddylistEntry(name, "Default Group", cidFrom, (byte) -1, buddylist.get(cidFrom).isVisible()));
                        addChar.getClient().announce(MaplePacketCreator.updateBuddyChannel(cidFrom, (byte) -1));
                    }
                    break;
            }
        }
    }

    public void loggedOff(String name, int characterId, int channel, int[] buddies) {
        updateBuddies(characterId, channel, buddies, true);
    }

    public void loggedOn(String name, int characterId, int channel, int[] buddies) {
        updateBuddies(characterId, channel, buddies, false);
    }

    private void updateBuddies(int characterId, int channel, int[] buddies, boolean offline) {
        for (int buddy : buddies) {
            MapleCharacter chr = getPlayerStorage().get(buddy);
            if (chr != null) {
                BuddylistEntry ble = chr.getBuddylist().get(characterId);
                if (ble != null && ble.isVisible()) {
                    int mcChannel;
                    if (offline) {
                        ble.setChannel((byte) -1);
                        mcChannel = -1;
                    } else {
                        ble.setChannel(channel);
                        mcChannel = (byte) (channel - 1);
                    }
                    chr.getBuddylist().put(ble);
                    chr.getClient().announce(MaplePacketCreator.updateBuddyChannel(ble.getCharacterId(), mcChannel));
                }
            }
        }
    }

    @Override
    public void dispose() {
        for (MapleCharacter player : getPlayers()) {
            player.saveToDB();
        }
        LOGGER.info("All players in world {} saved", getId());
        getChannels().forEach(MapleChannel::shutdown);
    }

    @Override
    public Collection<MapleCharacter> getPlayers() {
        return new ArrayList<>(playerStorage.values());
    }
}
