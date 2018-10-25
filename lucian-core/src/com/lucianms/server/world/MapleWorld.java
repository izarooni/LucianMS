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
import com.lucianms.server.Server;
import com.lucianms.server.channel.CharacterIdChannelPair;
import com.lucianms.server.channel.MapleChannel;
import com.lucianms.server.guild.MapleGuild;
import com.lucianms.server.guild.MapleGuildCharacter;
import com.lucianms.server.guild.MapleGuildSummary;
import tools.MaplePacketCreator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

/**
 * @author kevintjuh93
 */
public class MapleWorld {

    private int id, flag, exprate, droprate, mesorate, bossdroprate;
    private String eventmsg;
    private List<MapleChannel> channels = new ArrayList<>();

    private Map<Integer, MapleParty> parties = new HashMap<>();
    private Map<Integer, MapleMessenger> messengers = new HashMap<>();
    private Map<Integer, MapleGuildSummary> gsStore = new HashMap<>();
    private Map<Integer, MapleFamily> families = new LinkedHashMap<>();
    private Map<String, SAutoEvent> scheduledEvents = new HashMap<>();

    private AtomicInteger runningPartyId = new AtomicInteger(1);
    private AtomicInteger runningMessengerId = new AtomicInteger(1);

    private ManualPlayerEvent playerEvent;

    public MapleWorld(int world, int flag, String eventmsg, int exprate, int droprate, int mesorate, int bossdroprate) {
        this.id = world;
        this.flag = flag;
        this.eventmsg = eventmsg;
        this.exprate = exprate;
        this.droprate = droprate;
        this.mesorate = mesorate;
        this.bossdroprate = bossdroprate;
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

    public int getFlag() {
        return flag;
    }

    public void setFlag(byte b) {
        this.flag = b;
    }

    public String getEventMessage() {
        return eventmsg;
    }

    public void updateRates() {
        for (MapleChannel channel : channels) {
            channel.getPlayerStorage().forEach(MapleCharacter::setRates);
        }
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
        for (MapleChannel channel : channels) {
            MapleCharacter player = channel.getPlayerStorage().find(predicate);
            if (player != null) {
                return player;
            }
        }
        return null;
    }

    public MapleCharacter getPlayer(int playerID) {
        for (MapleChannel channel : channels) {
            MapleCharacter player = channel.getPlayerStorage().get(playerID);
            if (player != null) {
                return player;
            }
        }
        return null;
    }

    public void removePlayer(MapleCharacter chr) {
        channels.get(chr.getClient().getChannel() - 1).removePlayer(chr);
    }

    public int getId() {
        return id;
    }

    public void addFamily(int id, MapleFamily f) {
        families.putIfAbsent(id, f);
    }

    public ManualPlayerEvent getPlayerEvent() {
        return playerEvent;
    }

    public void setPlayerEvent(ManualPlayerEvent playerEvent) {
        this.playerEvent = playerEvent;
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
        MapleCharacter mc = getPlayer(cid);
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
            mc.getMap().broadcastMessage(mc, MaplePacketCreator.removePlayerFromMap(cid), false);
            mc.getMap().broadcastMessage(mc, MaplePacketCreator.spawnPlayerMapobject(mc), false);
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
            c = getPlayer(i);
            if (c != null) {
                c.getClient().announce(packet);
            }
        }
    }

    public MapleParty createParty(MaplePartyCharacter chrfor) {
        int partyid = runningPartyId.getAndIncrement();
        MapleParty party = new MapleParty(partyid, chrfor);
        parties.put(party.getId(), party);
        return party;
    }

    public MapleParty getParty(int partyid) {
        return parties.get(partyid);
    }

    public MapleParty disbandParty(int partyid) {
        return parties.remove(partyid);
    }

    public void updateParty(MapleParty party, PartyOperation operation, MaplePartyCharacter target) {
        for (MaplePartyCharacter partychar : party.getMembers()) {
            MapleCharacter chr = findPlayer(p -> p.getName().equalsIgnoreCase(partychar.getName()));
            if (chr != null) {
                if (operation == PartyOperation.DISBAND) {
                    chr.setParty(null);
                    chr.setMPC(null);
                } else {
                    chr.setParty(party);
                    chr.setMPC(partychar);
                }
                chr.getClient().announce(MaplePacketCreator.updateParty(chr.getClient().getChannel(), party, operation, target));
            }
        }
        switch (operation) {
            case LEAVE:
            case EXPEL:
                MapleCharacter chr = findPlayer(p -> p.getName().equalsIgnoreCase(target.getName()));
                if (chr != null) {
                    chr.getClient().announce(MaplePacketCreator.updateParty(chr.getClient().getChannel(), party, operation, target));
                    chr.setParty(null);
                    chr.setMPC(null);
                }
            default:
                break;
        }
    }

    public void updateParty(int partyid, PartyOperation operation, MaplePartyCharacter target) {
        MapleParty party = getParty(partyid);
        if (party == null) {
            throw new IllegalArgumentException("no party with the specified partyid exists");
        }
        switch (operation) {
            case JOIN:
                party.addMember(target);
                break;
            case EXPEL:
            case LEAVE:
                party.removeMember(target);
                break;
            case DISBAND:
                disbandParty(partyid);
                break;
            case SILENT_UPDATE:
            case LOG_ONOFF:
                party.updateMember(target);
                break;
            case CHANGE_LEADER:
                party.setLeader(target);
                break;
            default:
                System.out.println("Unhandeled updateParty operation " + operation.name());
        }
        updateParty(party, operation, target);
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
        MapleCharacter chr = getPlayer(id);
        if (chr != null) {
            channel = chr.getClient().getChannel();
        }
        return channel;
    }

    public void partyChat(MapleParty party, String chattext, String namefrom) {
        for (MaplePartyCharacter partychar : party.getMembers()) {
            if (!(partychar.getName().equals(namefrom))) {
                MapleCharacter chr = findPlayer(p -> p.getName().equalsIgnoreCase(partychar.getName()));
                if (chr != null) {
                    chr.getClient().announce(MaplePacketCreator.multiChat(namefrom, chattext, 1));
                }
            }
        }
    }

    public void buddyChat(int[] recipientCharacterIds, int cidFrom, String nameFrom, String chattext) {
        for (int characterId : recipientCharacterIds) {
            MapleCharacter chr = getPlayer(characterId);
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

    public void leaveMessenger(int messengerid, MapleMessengerCharacter target) {
        MapleMessenger messenger = getMessenger(messengerid);
        if (messenger == null) {
            throw new IllegalArgumentException("No messenger with the specified messengerid exists");
        }
        int position = messenger.getPositionByName(target.getName());
        messenger.removeMember(target);
        removeMessengerPlayer(messenger, position);
    }

    public void messengerInvite(String sender, int messengerid, String target, int fromchannel) {
        if (isConnected(target)) {
            MapleMessenger messenger = findPlayer(p -> p.getName().equalsIgnoreCase(target)).getMessenger();
            if (messenger == null) {
                findPlayer(p -> p.getName().equalsIgnoreCase(target)).getClient().announce(MaplePacketCreator.messengerInvite(sender, messengerid));
                MapleCharacter from = getChannel(fromchannel).getPlayerStorage().find(p -> p.getName().equalsIgnoreCase(sender));
                from.getClient().announce(MaplePacketCreator.messengerNote(target, 4, 1));
            } else {
                MapleCharacter from = getChannel(fromchannel).getPlayerStorage().find(p -> p.getName().equalsIgnoreCase(sender));
                from.getClient().announce(MaplePacketCreator.messengerChat(sender + " : " + target + " is already using Maple Messenger"));
            }
        }
    }

    public void addMessengerPlayer(MapleMessenger messenger, String namefrom, int fromchannel, int position) {
        for (MapleMessengerCharacter messengerchar : messenger.getMembers()) {
            MapleCharacter chr = findPlayer(p -> p.getName().equalsIgnoreCase(messengerchar.getName()));
            if (chr == null) {
                continue;
            }
            if (!messengerchar.getName().equals(namefrom)) {
                MapleCharacter from = getChannel(fromchannel).getPlayerStorage().find(p -> p.getName().equalsIgnoreCase(namefrom));
                chr.getClient().announce(MaplePacketCreator.addMessengerPlayer(namefrom, from, position, (byte) (fromchannel - 1)));
                from.getClient().announce(MaplePacketCreator.addMessengerPlayer(chr.getName(), chr, messengerchar.getPosition(), (byte) (messengerchar.getChannel() - 1)));
            } else {
                chr.getClient().announce(MaplePacketCreator.joinMessenger(messengerchar.getPosition()));
            }
        }
    }

    public void removeMessengerPlayer(MapleMessenger messenger, int position) {
        for (MapleMessengerCharacter messengerchar : messenger.getMembers()) {
            MapleCharacter chr = findPlayer(p -> p.getName().equalsIgnoreCase(messengerchar.getName()));
            if (chr != null) {
                chr.getClient().announce(MaplePacketCreator.removeMessengerPlayer(position));
            }
        }
    }

    public void messengerChat(MapleMessenger messenger, String chattext, String namefrom) {
        String from = "";
        String to1 = "";
        String to2 = "";
        for (MapleMessengerCharacter messengerchar : messenger.getMembers()) {
            if (!(messengerchar.getName().equals(namefrom))) {
                MapleCharacter chr = findPlayer(p -> p.getName().equalsIgnoreCase(messengerchar.getName()));
                if (chr != null) {
                    chr.getClient().announce(MaplePacketCreator.messengerChat(chattext));
                    if (to1.equals("")) {
                        to1 = messengerchar.getName();
                    } else if (to2.equals("")) {
                        to2 = messengerchar.getName();
                    }
                }
            } else {
                from = messengerchar.getName();
            }
        }
    }

    public void declineChat(String target, String namefrom) {
        if (isConnected(target)) {
            MapleCharacter chr = findPlayer(p -> p.getName().equalsIgnoreCase(target));
            if (chr != null && chr.getMessenger() != null) {
                chr.getClient().announce(MaplePacketCreator.messengerNote(namefrom, 5, 0));
            }
        }
    }

    public void updateMessenger(int messengerid, String namefrom, int fromchannel) {
        MapleMessenger messenger = getMessenger(messengerid);
        int position = messenger.getPositionByName(namefrom);
        updateMessenger(messenger, namefrom, position, fromchannel);
    }

    public void updateMessenger(MapleMessenger messenger, String namefrom, int position, int fromchannel) {
        for (MapleMessengerCharacter messengerchar : messenger.getMembers()) {
            MapleChannel ch = getChannel(fromchannel);
            if (!(messengerchar.getName().equals(namefrom))) {
                MapleCharacter chr = ch.getPlayerStorage().find(p -> p.getName().equalsIgnoreCase(messengerchar.getName()));
                if (chr != null) {
                    chr.getClient().announce(MaplePacketCreator.updateMessengerPlayer(namefrom, getChannel(fromchannel).getPlayerStorage().find(p -> p.getName().equalsIgnoreCase(namefrom)), position, (byte) (fromchannel - 1)));
                }
            }
        }
    }

    public void silentLeaveMessenger(int messengerid, MapleMessengerCharacter target) {
        MapleMessenger messenger = getMessenger(messengerid);
        if (messenger == null) {
            throw new IllegalArgumentException("No messenger with the specified messengerid exists");
        }
        messenger.addMember(target, target.getPosition());
    }

    public void joinMessenger(int messengerid, MapleMessengerCharacter target, String from, int fromchannel) {
        MapleMessenger messenger = getMessenger(messengerid);
        if (messenger == null) {
            throw new IllegalArgumentException("No messenger with the specified messengerid exists");
        }
        messenger.addMember(target, target.getPosition());
        addMessengerPlayer(messenger, from, fromchannel, target.getPosition());
    }

    public void silentJoinMessenger(int messengerid, MapleMessengerCharacter target, int position) {
        MapleMessenger messenger = getMessenger(messengerid);
        if (messenger == null) {
            throw new IllegalArgumentException("No messenger with the specified messengerid exists");
        }
        messenger.addMember(target, position);
    }

    public MapleMessenger createMessenger(MapleMessengerCharacter chrfor) {
        int messengerid = runningMessengerId.getAndIncrement();
        MapleMessenger messenger = new MapleMessenger(messengerid, chrfor);
        messengers.put(messenger.getId(), messenger);
        return messenger;
    }

    public boolean isConnected(String charName) {
        return findPlayer(p -> p.getName().equalsIgnoreCase(charName)) != null;
    }

    public void whisper(String sender, String target, int channel, String message) {
        if (isConnected(target)) {
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
        MapleCharacter addChar = getPlayer(cid);
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

    public void loggedOn(String name, int characterId, int channel, int buddies[]) {
        updateBuddies(characterId, channel, buddies, false);
    }

    private void updateBuddies(int characterId, int channel, int[] buddies, boolean offline) {
        for (int buddy : buddies) {
            MapleCharacter chr = getPlayer(buddy);
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

    public void broadcastMessage(int type, String content, Object... args) {
        channels.forEach(ch -> ch.getPlayerStorage().values().forEach(chr -> chr.sendMessage(type, content, args)));
    }

    public void setServerMessage(String message) {
        channels.forEach(ch -> ch.setServerMessage(message));
    }

    public void broadcastPacket(byte[] data) {
        channels.forEach(ch -> ch.broadcastPacket(data));
    }

    public void broadcastGMPacket(byte[] data) {
        channels.forEach(ch -> ch.getPlayerStorage().values().stream().filter(MapleCharacter::isGM).forEach(p -> p.announce(data)));
    }

    public final void shutdown() {
        getChannels().forEach(MapleChannel::shutdown);
    }
}
