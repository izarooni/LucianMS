package com.lucianms.events;

import com.lucianms.client.*;
import com.lucianms.client.BuddyList.BuddyAddResult;
import com.lucianms.client.BuddyList.BuddyOperation;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.world.MapleWorld;
import tools.MaplePacketCreator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static com.lucianms.client.BuddyList.BuddyOperation.ADDED;

/**
 * @author izarooni
 */
public class PlayerFriendsListModifyEvent extends PacketEvent {
    private static class CharacterIdNameBuddyCapacity extends CharacterNameAndId {
        private int buddyCapacity;

        CharacterIdNameBuddyCapacity(int id, String name, int buddyCapacity) {
            super(id, name);
            this.buddyCapacity = buddyCapacity;
        }

        int getBuddyCapacity() {
            return buddyCapacity;
        }
    }

    private void nextPendingRequest(MapleClient c) {
        CharacterNameAndId pendingBuddyRequest = c.getPlayer().getBuddylist().pollPendingRequest();
        if (pendingBuddyRequest != null) {
            c.announce(MaplePacketCreator.requestBuddylistAdd(pendingBuddyRequest.getId(), c.getPlayer().getId(), pendingBuddyRequest.getName()));
        }
    }

    private CharacterIdNameBuddyCapacity getCharacterIdAndNameFromDatabase(Connection con, String name) throws SQLException {
        CharacterIdNameBuddyCapacity ret;
        try (PreparedStatement ps = con.prepareStatement("SELECT id, name, buddyCapacity FROM characters WHERE name LIKE ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                ret = null;
                if (rs.next()) {
                    ret = new CharacterIdNameBuddyCapacity(rs.getInt("id"), rs.getString("name"), rs.getInt("buddyCapacity"));
                }
            }
        }
        return ret;
    }

    private String username;
    private String group;
    private byte action;
    private int playerID;

    @Override
    public void processInput(MaplePacketReader reader) {
        action = reader.readByte();
        switch (action) {
            case 1:
                username = reader.readMapleAsciiString();
                group = reader.readMapleAsciiString();
                if (group.length() > 16 || username.length() > 13 || username.equalsIgnoreCase(getClient().getPlayer().getName())) {
                    setCanceled(true);
                }
                break;
            case 2:
                playerID = reader.readInt();
                if (playerID == getClient().getPlayer().getId()) {
                    setCanceled(true);
                }
                break;
            case 3:
                playerID = reader.readInt();
                break;
        }
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        BuddyList buddylist = player.getBuddylist();
        switch (action) {
            case 1: {
                BuddylistEntry ble = buddylist.get(username);
                if (ble != null && !ble.isVisible() && group.equals(ble.getGroup())) {
                    player.sendMessage(1, "You already have \"{}\" on your Buddylist", ble.getName());
                } else if (buddylist.isFull() && ble == null) {
                    player.sendMessage(1, "Your buddylist is already full");
                } else if (ble == null) {
                    try (Connection con = getClient().getChannelServer().getConnection()) {
                        MapleWorld world = getClient().getWorldServer();
                        CharacterIdNameBuddyCapacity charWithId;
                        int channel;
                        MapleCharacter otherChar = getClient().getChannelServer().getPlayerStorage().find(p -> p.getName().equalsIgnoreCase(username));
                        if (otherChar != null) {
                            channel = getClient().getChannel();
                            charWithId = new CharacterIdNameBuddyCapacity(otherChar.getId(), otherChar.getName(), otherChar.getBuddylist().getCapacity());
                        } else {
                            channel = world.find(username);
                            charWithId = getCharacterIdAndNameFromDatabase(con, username);
                        }
                        if (charWithId != null) {
                            BuddyAddResult buddyAddResult = null;
                            if (channel != -1) {
                                buddyAddResult = world.requestBuddyAdd(username, getClient().getChannel(), player.getId(), player.getName());
                            } else {
                                try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) as buddyCount FROM buddies WHERE characterid = ? AND pending = 0")) {
                                    ps.setInt(1, charWithId.getId());
                                    try (ResultSet rs = ps.executeQuery()) {
                                        if (!rs.next()) {
                                            throw new RuntimeException("Result set expected");
                                        } else if (rs.getInt("buddyCount") >= charWithId.getBuddyCapacity()) {
                                            buddyAddResult = BuddyAddResult.BUDDYLIST_FULL;
                                        }
                                    }
                                }
                                try (PreparedStatement ps = con.prepareStatement("SELECT pending FROM buddies WHERE characterid = ? AND buddyid = ?")) {
                                    ps.setInt(1, charWithId.getId());
                                    ps.setInt(2, player.getId());
                                    try (ResultSet rs = ps.executeQuery()) {
                                        if (rs.next()) {
                                            buddyAddResult = BuddyAddResult.ALREADY_ON_LIST;
                                        }
                                    }
                                }
                            }
                            if (buddyAddResult == BuddyAddResult.BUDDYLIST_FULL) {
                                getClient().announce(MaplePacketCreator.serverNotice(1, "\"" + username + "\"'s Buddylist is full"));
                            } else {
                                int displayChannel;
                                displayChannel = -1;
                                int otherCid = charWithId.getId();
                                if (buddyAddResult == BuddyAddResult.ALREADY_ON_LIST && channel != -1) {
                                    displayChannel = channel;
                                    notifyTargetPlayer(getClient(), channel, otherCid, ADDED);
                                } else if (buddyAddResult != BuddyAddResult.ALREADY_ON_LIST && channel == -1) {
                                    try (PreparedStatement ps = con.prepareStatement("INSERT INTO buddies (characterid, `buddyid`, `pending`) VALUES (?, ?, 1)")) {
                                        ps.setInt(1, charWithId.getId());
                                        ps.setInt(2, player.getId());
                                        ps.executeUpdate();
                                    }
                                }
                                buddylist.put(new BuddylistEntry(charWithId.getName(), group, otherCid, displayChannel, true));
                                getClient().announce(MaplePacketCreator.updateBuddylist(buddylist.getBuddies()));
                            }
                        } else {
                            getClient().announce(MaplePacketCreator.serverNotice(1, "A character called \"" + username + "\" does not exist"));
                        }
                    } catch (SQLException e) {
                        getLogger().error("Unable to create database connection: {}", e.getMessage());
                    }
                } else {
                    ble.changeGroup(group);
                    getClient().announce(MaplePacketCreator.updateBuddylist(buddylist.getBuddies()));
                }
                break;
            }
            case 2: {
                if (!buddylist.isFull()) {
                    int channel = getClient().getWorldServer().find(playerID);
                    String otherName = null;
                    MapleCharacter otherChar = getClient().getChannelServer().getPlayerStorage().get(playerID);
                    if (otherChar == null) {
                        try (Connection con = getClient().getChannelServer().getConnection();
                             PreparedStatement ps = con.prepareStatement("SELECT name FROM characters WHERE id = ?")) {
                            ps.setInt(1, playerID);
                            try (ResultSet rs = ps.executeQuery()) {
                                if (rs.next()) {
                                    otherName = rs.getString("name");
                                }
                            }
                        } catch (SQLException e) {
                            getLogger().error("Unable to select from characters: {}", e.getMessage());
                        }
                    } else {
                        otherName = otherChar.getName();
                    }
                    if (otherName != null) {
                        buddylist.put(new BuddylistEntry(otherName, "Default Group", playerID, channel, true));
                        getClient().announce(MaplePacketCreator.updateBuddylist(buddylist.getBuddies()));
                        notifyTargetPlayer(getClient(), channel, playerID, ADDED);
                    }
                }
                nextPendingRequest(getClient());
                break;
            }
            case 3: {
                if (buddylist.containsVisible(playerID)) {
                    notifyTargetPlayer(getClient(), getClient().getWorldServer().find(playerID), playerID, BuddyOperation.DELETED);
                }
                buddylist.remove(playerID);
                getClient().announce(MaplePacketCreator.updateBuddylist(player.getBuddylist().getBuddies()));
                nextPendingRequest(getClient());
                break;
            }
        }
        return null;
    }

    private void notifyTargetPlayer(MapleClient c, int tChannelID, int tPlayerID, BuddyOperation operation) {
        MapleCharacter player = getClient().getPlayer();
        if (tChannelID != -1) {
            getClient().getWorldServer().buddyChanged(tPlayerID, player.getId(), player.getName(), c.getChannel(), operation);
        }
    }
}
