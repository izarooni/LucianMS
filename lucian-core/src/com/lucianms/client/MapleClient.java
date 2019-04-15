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
package com.lucianms.client;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.lucianms.client.inventory.MapleInventoryType;
import com.lucianms.io.scripting.npc.NPCConversationManager;
import com.lucianms.io.scripting.npc.NPCScriptManager;
import com.lucianms.io.scripting.quest.QuestActionManager;
import com.lucianms.io.scripting.quest.QuestScriptManager;
import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.server.MapleTrade;
import com.lucianms.server.Server;
import com.lucianms.server.channel.MapleChannel;
import com.lucianms.server.guild.MapleGuild;
import com.lucianms.server.guild.MapleGuildCharacter;
import com.lucianms.server.maps.FieldLimit;
import com.lucianms.server.maps.HiredMerchant;
import com.lucianms.server.quest.MapleQuest;
import com.lucianms.server.world.MapleParty;
import com.lucianms.server.world.MaplePartyCharacter;
import com.lucianms.server.world.MapleWorld;
import com.lucianms.server.world.PartyOperation;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.*;

import java.io.IOException;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MapleClient implements Disposable {

    public static final AttributeKey<MapleClient> CLIENT_KEY = AttributeKey.newInstance(MapleClient.class.getName());
    private static final Logger LOGGER = LoggerFactory.getLogger(MapleClient.class);
    private final Lock mutex = new ReentrantLock(true);
    private MapleAESOFB send;
    private MapleAESOFB receive;
    private Channel session;
    private MapleCharacter player;
    private Calendar birthday;
    private long sessionId;
    private long lastPong;
    private int channel;
    private int ID;
    private int world;
    private int gmlevel;
    private int pinattempt;
    private int picattempt;
    private int voteTime;
    private byte characterSlots;
    private byte loginAttempts;
    private byte gender;
    private LoginState loginState;
    private volatile boolean disconnecting;
    private String pin;
    private String pic;
    private String hwid;
    private String lastKnownIP;
    private String accountName;
    private Set<String> macs;

    private long discordId;
    private String discordKey;

    public MapleClient(MapleAESOFB send, MapleAESOFB receive, Channel session) {
        this.send = send;
        this.receive = receive;
        this.session = session;

        gender = 10;
        voteTime = -1;
        characterSlots = 3;
        loginState = LoginState.LogOut;
        macs = new HashSet<>();
    }

    public MapleAESOFB getSendCrypto() {
        return send;
    }

    public MapleAESOFB getReceiveCrypto() {
        return receive;
    }

    public Channel getSession() {
        return session;
    }

    public void setSession(Channel session) {
        this.session = session;
    }

    public MapleCharacter getPlayer() {
        return player;
    }

    public void setPlayer(MapleCharacter player) {
        this.player = player;
    }

    public void sendCharList(int server) {
        announce(MaplePacketCreator.getCharList(this, server));
    }

    /**
     * Looks for a player in the database with an ID that matches the account ID of this client
     *
     * @param playerId ID of a character
     * @return true if the account the specified character belongs to this account, false otherwise
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isPlayerBelonging(int playerId) {
        try (Connection con = Server.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT count(*) AS total FROM characters WHERE id = ? AND accountid = ? ")) {
            ps.setInt(1, playerId);
            ps.setInt(2, getAccID());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total") == 1;
                }
            }
        } catch (SQLException e) {
            System.err.println(String.format("Unable to verify character ownership. Character(%s) Account(%s)", MapleCharacter.getNameById(playerId), getAccountName()));
        }
        return false;
    }

    public List<MapleCharacter> loadCharacters(int serverId) {
        try (Connection con = Server.getConnection()) {
            List<MapleCharacter> ret = new ArrayList<>();
            for (Pair<Integer, String> pair : loadCharactersInternal(serverId)) {
                MapleCharacter load = MapleCharacter.loadCharFromDB(con, pair.getLeft(), this, false);
                if (load != null) {
                    ret.add(load);
                } else {
                    LOGGER.error("Unable to load character {} for account {} - method loadCharacters(int serverId)", pair.getLeft() + "/" + pair.getRight(), getAccountName() + "/" + getAccID());
                }
            }
            return ret;
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public List<Pair<Integer, String>> loadCharactersInternal(int serverId) {
        List<Pair<Integer, String>> ret = new ArrayList<>();
        try (Connection con = Server.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT id, name FROM characters WHERE accountid = ? AND world = ?")) {
            ps.setInt(1, getAccID());
            ps.setInt(2, serverId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ret.add(new Pair<>(rs.getInt("id"), rs.getString("name")));
                }
            }
            return ret;
        } catch (SQLException e) {
            LOGGER.info("Unable to load account ID: '{}', Name: {} character id and username information", ID, accountName, e);
            return Collections.emptyList();
        }
    }

    public boolean hasBannedIP() {
        boolean ret = false;
        try (Connection con = Server.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM ipbans WHERE ? LIKE CONCAT(ip, '%')")) {
            ps.setString(1, getRemoteAddress());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    ret = true;
                }
            }
        } catch (SQLException ignored) {
        }
        return ret;
    }

    public void resetVoteTime() {
        voteTime = -1;
    }

    public boolean hasBannedHWID() {
        if (hwid == null) {
            return false;
        }

        try (Connection con = Server.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM hwidbans WHERE hwid LIKE ?")) {
            ps.setString(1, hwid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    if (rs.getInt(1) > 0) {
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean hasBannedMac() {
        if (macs.isEmpty()) {
            return false;
        }
        boolean ret = false;
        int i;
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM macbans WHERE mac IN (");
        for (i = 0; i < macs.size(); i++) {
            sql.append("?");
            if (i != macs.size() - 1) {
                sql.append(", ");
            }
        }
        sql.append(")");
        try (Connection con = Server.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            i = 0;
            for (String mac : macs) {
                i++;
                ps.setString(i, mac);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                if (rs.getInt(1) > 0) {
                    ret = true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    private void loadHWIDIfNescessary(Connection con) throws SQLException {
        if (hwid == null) {
            try (PreparedStatement ps = con.prepareStatement("SELECT hwid FROM accounts WHERE id = ?")) {
                ps.setInt(1, ID);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        hwid = rs.getString("hwid");
                    }
                }
            }
        }
    }

    private void loadMacsIfNescessary(Connection con) throws SQLException {
        if (macs.isEmpty()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT macs FROM accounts WHERE id = ?")) {
                ps.setInt(1, ID);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        for (String mac : rs.getString("macs").split(", ")) {
                            if (!mac.equals("")) {
                                macs.add(mac);
                            }
                        }
                    }
                }
            }
        }
    }

    public void banHWID() {
        try (Connection con = Server.getConnection();
             PreparedStatement ps = con.prepareStatement("INSERT INTO hwidbans (hwid) VALUES (?)")) {
            loadHWIDIfNescessary(con);
            ps.setString(1, hwid);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void banMacs() {
        try (Connection con = Server.getConnection()) {
            loadMacsIfNescessary(con);
            List<String> filtered = new LinkedList<>();
            try (PreparedStatement ps = con.prepareStatement("SELECT filter FROM macfilters"); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    filtered.add(rs.getString("filter"));
                }
            }
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO macbans (mac) VALUES (?)")) {
                for (String mac : macs) {
                    boolean matched = false;
                    for (String filter : filtered) {
                        if (mac.matches(filter)) {
                            matched = true;
                            break;
                        }
                    }
                    if (!matched) {
                        ps.setString(1, mac);
                        ps.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
        try (Connection con = Server.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE accounts SET pin = ? WHERE id = ?")) {
            ps.setString(1, pin);
            ps.setInt(2, ID);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean checkPin(String other) {
        pinattempt++;
        if (pinattempt > 5) {
            getSession().close();
        }
        if (pin.equals(other)) {
            pinattempt = 0;
            return true;
        }
        return false;
    }

    public String getPic() {
        return pic;
    }

    public void setPic(String pic) {
        this.pic = pic;
        try (Connection con = Server.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE accounts SET pic = ? WHERE id = ?")) {
            ps.setString(1, pic);
            ps.setInt(2, ID);
            ps.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

    public boolean checkPic(String other) {
        picattempt++;
        if (picattempt > 5) {
            getSession().close();
        }
        if (pic.equals(other)) {
            picattempt = 0;
            return true;
        }
        return false;
    }

    public int getLoginResponse(String username, String password) {
        if (loginAttempts++ > 5) {
            getSession().close();
            return 3;
        }
        int loginResponse = 5;
        try (Connection con = Server.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM accounts WHERE name = ?")) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        if (rs.getByte("banned") == 1) {
                            return 3;
                        }
                        ID = rs.getInt("id");
                        gmlevel = rs.getInt("gm");
                        pin = rs.getString("pin");
                        pic = rs.getString("pic");
                        gender = rs.getByte("gender");
                        characterSlots = rs.getByte("characterslots");
                        lastKnownIP = rs.getString("ip");
                        String cryptoPassword = rs.getString("password");
                        String salt = rs.getString("salt");
                        byte tos = rs.getByte("tos"); // who the fuck cares
                        if (BCrypt.verifyer().verify(password.getBytes(), cryptoPassword.getBytes()).verified) {
                            loginResponse = 0;
                        } else if (password.equals(cryptoPassword)
                                || StringUtil.checkHash(cryptoPassword, "SHA-1", password)
                                || StringUtil.checkHash(cryptoPassword, "SHA-512", password + salt)) {
                            loginResponse = 0;
                            try (PreparedStatement fuck = con.prepareStatement("update accounts set password = ? where id = ?")) {
                                String bcryptPassword = BCrypt.with(BCrypt.Version.VERSION_2Y).hashToString(10, password.toCharArray());
                                fuck.setString(1, bcryptPassword);
                                fuck.setInt(1, ID);
                                fuck.executeUpdate();
                            }
                        } else {
                            loginResponse = 4;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return loginResponse;
    }

    public Calendar getTempBanCalendar() {
        final Calendar lTempban = Calendar.getInstance();
        try (Connection con = Server.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT `tempban` FROM accounts WHERE id = ?")) {
            ps.setInt(1, getAccID());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                long blubb = rs.getLong("tempban");
                if (blubb == 0) { // basically if timestamp in db is 0000-00-00
                    return null;
                }
                lTempban.setTimeInMillis(rs.getTimestamp("tempban").getTime());
                return lTempban;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;//why oh why!?!
    }

    public void updateHWID(String newHwid) {
        String[] split = newHwid.split("_");
        if (split.length > 1 && split[1].length() == 8) {
            StringBuilder hwid = new StringBuilder();
            String convert = split[1];

            int len = convert.length();
            for (int i = len - 2; i >= 0; i -= 2) {
                hwid.append(convert, i, i + 2);
            }
            hwid.insert(4, "-");

            this.hwid = hwid.toString();

            try (Connection con = Server.getConnection();
                 PreparedStatement ps = con.prepareStatement("UPDATE accounts SET hwid = ? WHERE id = ?")) {
                ps.setString(1, this.hwid);
                ps.setInt(2, ID);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            disconnect(); // Invalid HWID...
        }
    }

    public void updateMacs(String macData) {
        macs.addAll(Arrays.asList(macData.split(", ")));
        StringBuilder newMacData = new StringBuilder();
        Iterator<String> iter = macs.iterator();
        while (iter.hasNext()) {
            String cur = iter.next();
            newMacData.append(cur);
            if (iter.hasNext()) {
                newMacData.append(", ");
            }
        }
        try (Connection con = Server.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE accounts SET macs = ? WHERE id = ?")) {
            ps.setString(1, newMacData.toString());
            ps.setInt(2, ID);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int getAccID() {
        return ID;
    }

    public void setAccID(int accId) {
        this.ID = accId;
    }

    public void setLoginState(LoginState loginState) {
        this.loginState = loginState;
        try (Connection con = Server.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("update accounts set loggedin = ?, lastlogin = current_timestamp() where id = ?")) {
                ps.setInt(1, loginState.ordinal());
                ps.setInt(2, getAccID());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to update login state for account {}", this, e);
        }
    }

    public LoginState getLoginState() {
        return loginState;
    }

    public LoginState checkLoginState() {
        try (Connection con = Server.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("select loggedin, lastlogin, UNIX_TIMESTAMP(birthday) as birthday from accounts where id = ?")) {
                ps.setInt(1, getAccID());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new RuntimeException();
                    }
                    birthday = Calendar.getInstance();
                    long unixBirthday = rs.getLong("birthday");
                    if (unixBirthday > 0) {
                        birthday.setTimeInMillis(unixBirthday * 1000);
                    }
                    LoginState state = LoginState.values()[rs.getInt("loggedin")];
                    switch (state) {
                        case LogOut:
                            break;
                        case Transfer:
                            break;
                        case Login:
                            if (player == null) {
                                setLoginState(LoginState.LogOut);
                            }
                            break;
                    }
                    return state;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean checkBirthDate(Calendar date) {
        return date.get(Calendar.YEAR) == birthday.get(Calendar.YEAR) && date.get(Calendar.MONTH) == birthday.get(Calendar.MONTH) && date.get(Calendar.DAY_OF_MONTH) == birthday.get(Calendar.DAY_OF_MONTH);
    }

    @Override
    public void dispose() {
        NPCScriptManager.dispose(this);
        QuestScriptManager.dispose(this);
    }

    public final void disconnect() {
        if (disconnecting) {
            return;
        }
        disconnecting = true;
        final MapleWorld world = getWorldServer();
        final MapleCharacter player = getPlayer();
        if (player != null) {
            player.getGenericEvents().forEach(e -> e.onPlayerDisconnect(player));

            try {
                for (MapleQuestStatus status : player.getStartedQuests()) {
                    //This is for those quests that you have to stay logged in for a certain amount of time
                    MapleQuest quest = status.getQuest();
                    if (quest.getTimeLimit() > 0) {
                        MapleQuestStatus newStatus = new MapleQuestStatus(quest, MapleQuestStatus.Status.NOT_STARTED);
                        newStatus.setForfeited(player.getQuest(quest).getForfeited() + 1);
                        player.updateQuest(newStatus);
                    }
                }

                Functions.requireNotNull(player.getMessenger(), m -> m.removeMember(player));
                int channelID = (getLoginState() == LoginState.Transfer) ? getChannel() : -1;
                MapleGuild guild = player.getGuild();
                if (guild != null) {
                    Server.setGuildMemberOnline(player.getMGC(), (getLoginState() == LoginState.Transfer), channelID);
                }
                MapleParty party = player.getParty();
                if (party != null) {
                    MaplePartyCharacter mpc = player.getMPC();
                    mpc.setOnline(false);
                    mpc.setChannel(-1);
                    world.updateParty(party.getId(), PartyOperation.LOG_ONOFF, mpc);
                    if (party.getLeader().getId() == mpc.getId()) {
                        var newLeader = party.getMembers().stream().filter(p -> p.getPlayer() != null).findAny();
                        newLeader.ifPresent(maplePartyCharacter -> world.updateParty(party.getId(), PartyOperation.CHANGE_LEADER, maplePartyCharacter));
                    }
                }
                BuddyList friends = player.getBuddylist();
                if (getLoginState() == LoginState.Login) {
                    world.loggedOff(player.getName(), player.getId(), channelID, friends.getBuddyIds());
                } else if (getLoginState() == LoginState.Transfer) {
                    world.loggedOn(player.getName(), player.getId(), channelID, friends.getBuddyIds());
                }
            } catch (Throwable t) {
                // whatever happens here, we still want to save the player.
                t.printStackTrace();
            }
            getWorldServer().removePlayer(player);
            player.dispose();
            dispose();
        }
        if (checkLoginState() != LoginState.Transfer) {
            setLoginState(LoginState.LogOut);
            session.attr(CLIENT_KEY).set(null);
            session.close();
        }
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public MapleChannel getChannelServer() {
        return Server.getChannel(world, channel);
    }

    public MapleWorld getWorldServer() {
        return Server.getWorld(world);
    }

    public MapleChannel getChannelServer(byte channel) {
        return Server.getChannel(world, channel);
    }

    public boolean deleteCharacter(int cid) {
        MapleCharacter player = Server.getWorld(0).getPlayer(cid);
        if (player != null) {
            player.getClient().disconnect();
            disconnect();
            return false; //DC both and return, fuck that
        }
        try (Connection con = Server.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT id, guildid, guildrank, name, allianceRank FROM characters WHERE id = ? AND accountid = ?")) {
                ps.setInt(1, cid);
                ps.setInt(2, ID);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return false;
                    }
                    if (rs.getInt("guildid") > 0) {
                        try {
                            Server.deleteGuildCharacter(new MapleGuildCharacter(cid, 0, rs.getString("name"), (byte) -1, (byte) -1, 0, rs.getInt("guildrank"), rs.getInt("guildid"), false, rs.getInt("allianceRank")));
                        } catch (Exception re) {
                            return false;
                        }
                    }
                }
            }

            try (PreparedStatement ps = con.prepareStatement("DELETE FROM wishlists WHERE charid = ?")) {
                ps.setInt(1, cid);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM characters WHERE id = ?")) {
                ps.setInt(1, cid);
                ps.executeUpdate();
            }
            String[] toDel = {"famelog", "inventoryitems", "keymap", "queststatus", "savedlocations", "skillmacros", "skills", "eventstats"};
            for (String s : toDel) {
                MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM `" + s + "` WHERE characterid = ?", cid);
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public int getWorld() {
        return world;
    }

    public void setWorld(int world) {
        this.world = world;
    }

    public void pongReceived() {
        lastPong = System.currentTimeMillis();
    }

    public void sendPing() {
        final long then = System.currentTimeMillis();
        announce(MaplePacketCreator.getPing());
        TaskExecutor.createTask(new Runnable() {

            @Override
            public void run() {
                if (lastPong < then) {
                    if (getSession() != null && getSession().isActive()) {
                        getSession().close();
                    }
                }
            }
        }, 1000 * 60 * 3);
    }

    public String getLastKnownIP() {
        return lastKnownIP;
    }

    public String getRemoteAddress() {
        return session.remoteAddress().toString().substring(1).split(":")[0];
    }

    public String getHWID() {
        return hwid;
    }

    public Set<String> getMacs() {
        return Collections.unmodifiableSet(macs);
    }

    public int getGMLevel() {
        return gmlevel;
    }

    public NPCConversationManager getCM() {
        return NPCScriptManager.getConversationManager(this);
    }

    public QuestActionManager getQM() {
        return QuestScriptManager.getActionManager(this);
    }

    public boolean acceptToS() {
        if (accountName == null) {
            return true;
        }
        try (Connection con = Server.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT `tos` FROM accounts WHERE id = ?")) {
                ps.setInt(1, ID);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        if (rs.getByte("tos") == 1) {
                            return true;
                        }
                    }
                }
            }
            try (PreparedStatement ps = con.prepareStatement("UPDATE accounts SET tos = 1 WHERE id = ?")) {
                ps.setInt(1, ID);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public int getVotePoints() {
        try (Connection con = Server.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT votepoints FROM accounts WHERE id = ?")) {
            ps.setInt(1, ID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("votepoints");
                }
            }
        } catch (SQLException e) {
            LOGGER.warn("Unable to retrieve vote points for account {} player {}", getAccountName(), ((player != null) ? player.getName() : "N/A"), e);
        }
        return 0;
    }

    public void setVotePoints(int n) {
        try (Connection con = Server.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE accounts SET votepoints = ? WHERE id = ?")) {
            ps.setInt(1, n);
            ps.setInt(2, ID);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.warn("Unable to update vote points for account {} player {}", getAccountName(), ((player != null) ? player.getName() : "N/A"), e);
        }
    }

    public void addVotePoints(int n) {
        setVotePoints(getVotePoints() + n);
    }

    public final Lock getLock() {
        return mutex;
    }

    public short getCharacterSlots() {
        return characterSlots;
    }

    public boolean gainCharacterSlot() {
        if (characterSlots < 15) {
            try (Connection con = Server.getConnection();
                 PreparedStatement ps = con.prepareStatement("UPDATE accounts SET characterslots = ? WHERE id = ?")) {
                ps.setInt(1, this.characterSlots += 1);
                ps.setInt(2, ID);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    public final byte getGReason() {
        try (Connection con = Server.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT `greason` FROM `accounts` WHERE id = ?")) {
            ps.setInt(1, ID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getByte("greason");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public byte getGender() {
        return gender;
    }

    public void setGender(byte m) {
        this.gender = m;
        try (Connection con = Server.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE accounts SET gender = ? WHERE id = ?")) {
            ps.setByte(1, gender);
            ps.setInt(2, ID);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void announce(byte[] packet) {
        session.writeAndFlush(packet);
    }

    public void changeChannel(int channel) {
        if (player.isBanned()) {
            disconnect();
            return;
        }
        if (!player.isAlive() || FieldLimit.CHANGECHANNEL.check(player.getMap().getFieldLimit())) {
            announce(MaplePacketCreator.enableActions());
            return;
        }
        String[] socket = getWorldServer().getChannel(channel).getIP().split(":");
        if (player.getTrade() != null) {
            MapleTrade.cancelTrade(getPlayer());
        }

        HiredMerchant merchant = player.getHiredMerchant();
        if (merchant != null) {
            if (merchant.isOwner(getPlayer())) {
                merchant.setOpen(true);
            } else {
                merchant.removeVisitor(getPlayer());
            }
        }
        Server.getPlayerBuffStorage().put(player.getId(), player.getAllBuffs());
        player.cancelBuffEffects();
        player.cancelMagicDoor();
        player.saveCooldowns();
        //Canceling mounts? Noty
        if (player.getBuffedValue(MapleBuffStat.PUPPET) != null) {
            player.cancelEffectFromBuffStat(MapleBuffStat.PUPPET);
        }
        if (player.getBuffedValue(MapleBuffStat.COMBO) != null) {
            player.cancelEffectFromBuffStat(MapleBuffStat.COMBO);
        }
        player.getInventory(MapleInventoryType.EQUIPPED).checked(false); //test
        player.getMap().removePlayer(player);
        player.getClient().getChannelServer().removePlayer(player);
        player.getClient().setLoginState(LoginState.Transfer);
        try {
            announce(MaplePacketCreator.getChannelChange(InetAddress.getByName(socket[0]), Integer.parseInt(socket[1])));
        } catch (IOException e) {
            LOGGER.error("Unable to change to channel {} from {} for user {} player {}", channel, this.channel, getAccountName(), player.getName());
        }
    }

    public long getSessionId() {
        return this.sessionId;
    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    public int getDonationPoints() {
        try (Connection con = Server.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT donationpoints FROM accounts WHERE id = ?")) {
            ps.setInt(1, ID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("donationpoints");
                }
            }
        } catch (SQLException e) {
            LOGGER.warn("Unable to retrieve donation points for account {} player {}", getAccountName(), ((player != null) ? player.getName() : "N/A"), e);
        }
        return 0;
    }

    public void setDonationPoints(int n) {
        try (Connection con = Server.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE accounts SET donationpoints = ? WHERE id = ?")) {
            ps.setInt(1, n);
            ps.setInt(2, ID);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.warn("Unable to update donation points for account {} player {}", getAccountName(), ((player != null) ? player.getName() : "N/A"), e);
        }
    }

    public void addDonationPoints(int n) {
        setDonationPoints(getDonationPoints() + n);
    }

    public long getDiscordId() {
        return discordId;
    }

    public void setDiscordId(long discordId) {
        this.discordId = discordId;
    }

    public String getDiscordKey() {
        return discordKey;
    }

    public void setDiscordKey(String discordKey) {
        this.discordKey = discordKey;
    }
}
