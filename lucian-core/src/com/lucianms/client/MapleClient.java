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

import com.lucianms.client.inventory.MapleInventoryType;
import com.lucianms.io.scripting.npc.NPCConversationManager;
import com.lucianms.io.scripting.npc.NPCScriptManager;
import com.lucianms.io.scripting.quest.QuestActionManager;
import com.lucianms.io.scripting.quest.QuestScriptManager;
import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.server.MapleMiniGame;
import com.lucianms.server.MaplePlayerShop;
import com.lucianms.server.MapleTrade;
import com.lucianms.server.Server;
import com.lucianms.server.channel.MapleChannel;
import com.lucianms.server.guild.MapleGuild;
import com.lucianms.server.guild.MapleGuildCharacter;
import com.lucianms.server.maps.FieldLimit;
import com.lucianms.server.maps.HiredMerchant;
import com.lucianms.server.maps.MapleMap;
import com.lucianms.server.quest.MapleQuest;
import com.lucianms.server.world.*;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.HexTool;
import tools.MapleAESOFB;
import tools.MaplePacketCreator;
import tools.Pair;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MapleClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(MapleClient.class);
    private static final long TRANSITION_TIMEOUT = 60000;

    public static final int LOGIN_NOTLOGGEDIN = 0;
    public static final int LOGIN_SERVER_TRANSITION = 1;
    public static final int LOGIN_LOGGEDIN = 2;
    public static final AttributeKey<MapleClient> CLIENT_KEY = AttributeKey.newInstance(MapleClient.class.getName());

    private final Lock mutex = new ReentrantLock(true);
    private MapleAESOFB send;
    private MapleAESOFB receive;
    private Channel session;
    private MapleCharacter player;
    private int channel = 1;
    private int accId = 0;
    private boolean loggedIn = false;
    private boolean serverTransition = false;
    private Calendar birthday = null;
    private String accountName = null;
    private int world;
    private long lastPong;
    private int gmlevel;
    private Set<String> macs = new HashSet<>();
    private byte characterSlots = 3;
    private byte loginattempt = 0;
    private String pin = null;
    private int pinattempt = 0;
    private String pic = null;
    private String hwid = null;
    private int picattempt = 0;
    private byte gender = -1;
    private boolean disconnecting = false;
    private int voteTime = -1;
    private long sessionId;
    private String lastKnownIP = null;

    private long discordId = 0;
    private String discordKey = null;

    public MapleClient(MapleAESOFB send, MapleAESOFB receive, Channel session) {
        this.send = send;
        this.receive = receive;
        this.session = session;
    }

    private static boolean checkHash(String hash, String type, String password) {
        try {
            MessageDigest digester = MessageDigest.getInstance(type);
            digester.update(password.getBytes("UTF-8"), 0, password.length());
            return HexTool.toString(digester.digest()).replace(" ", "").toLowerCase().equals(hash);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw new RuntimeException("Encoding the string failed", e);
        }
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
    public boolean playerBelongs(int playerId) {
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
            LOGGER.info("Unable to load account ID: '{}', Name: {} character id and username information", accId, accountName, e);
            return Collections.emptyList();
        }
    }

    public boolean isLoggedIn() {
        return loggedIn;
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
                ps.setInt(1, accId);
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
                ps.setInt(1, accId);
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
            ps.setInt(2, accId);
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
            ps.setInt(2, accId);
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

    public int login(String login, String pwd) {
        int loginok = 5;
        try (Connection con = Server.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT id, password, salt, gender, banned, gm, pin, pic, characterslots, tos, ip FROM accounts WHERE name = ?")) {
                ps.setString(1, login);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        if (rs.getByte("banned") == 1) {
                            return 3;
                        }
                        accId = rs.getInt("id");
                        gmlevel = rs.getInt("gm");
                        pin = rs.getString("pin");
                        pic = rs.getString("pic");
                        gender = rs.getByte("gender");
                        gender = (gender == 10) ? 0 : gender;
                        characterSlots = rs.getByte("characterslots");
                        lastKnownIP = rs.getString("ip");
                        String passhash = rs.getString("password");
                        String salt = rs.getString("salt");
                        byte tos = rs.getByte("tos");
                        if (pwd.equals(passhash) || checkHash(passhash, "SHA-1", pwd) || checkHash(passhash, "SHA-512", pwd + salt)) {
                            if (tos == 0) {
                                loginok = 23;
                            } else {
                                loginok = 0;
                            }
                        } else {
                            loggedIn = false;
                            loginok = 4;
                        }
                    }
                }
            }
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO iplog (accountid, ip) VALUES (?, ?)")) {
                ps.setInt(1, accId);
                ps.setString(2, getRemoteAddress());
                ps.executeUpdate();
            }
            if (loginok == 0) {
                loginattempt = 0;
                try (PreparedStatement ps = con.prepareStatement("update accounts set ip = ? where id = ?")) {
                    ps.setString(1, getRemoteAddress());
                    ps.setInt(2, getAccID());
                    ps.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } else if (loginattempt > 4) {
                getSession().close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return loginok;
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
                hwid.append(convert.substring(i, i + 2));
            }
            hwid.insert(4, "-");

            this.hwid = hwid.toString();

            try (Connection con = Server.getConnection();
                 PreparedStatement ps = con.prepareStatement("UPDATE accounts SET hwid = ? WHERE id = ?")) {
                ps.setString(1, this.hwid);
                ps.setInt(2, accId);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            disconnect(false); // Invalid HWID...
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
            ps.setInt(2, accId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int getAccID() {
        return accId;
    }

    public void setAccID(int accId) {
        this.accId = accId;
    }

    public void updateLoginState(int newstate) {
        try (Connection con = Server.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE accounts SET loggedin = ?, lastlogin = CURRENT_TIMESTAMP() WHERE id = ?")) {
            ps.setInt(1, newstate);
            ps.setInt(2, getAccID());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (newstate == LOGIN_NOTLOGGEDIN) {
            loggedIn = false;
            serverTransition = false;
        } else {
            serverTransition = (newstate == LOGIN_SERVER_TRANSITION);
            loggedIn = !serverTransition;
        }
    }

    public int getLoginState() {  // 0 = LOGIN_NOTLOGGEDIN, 1= LOGIN_SERVER_TRANSITION, 2 = LOGIN_LOGGEDIN
        try (Connection con = Server.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT loggedin, lastlogin, UNIX_TIMESTAMP(birthday) AS birthday FROM accounts WHERE id = ?")) {
            ps.setInt(1, getAccID());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new RuntimeException("getLoginState - MapleClient");
                }
                birthday = Calendar.getInstance();
                long blubb = rs.getLong("birthday");
                if (blubb > 0) {
                    birthday.setTimeInMillis(blubb * 1000);
                }
                int state = rs.getInt("loggedin");
                if (state == LOGIN_SERVER_TRANSITION) {
                    long lastlogin = rs.getTimestamp("lastlogin").getTime() + TRANSITION_TIMEOUT;
                    if (lastlogin < System.currentTimeMillis()) {
                        state = LOGIN_NOTLOGGEDIN;
                        updateLoginState(LOGIN_NOTLOGGEDIN);
                    }
                } else if (state == LOGIN_LOGGEDIN && player == null) {
                    updateLoginState(LOGIN_LOGGEDIN);
                }
                if (state == LOGIN_LOGGEDIN) {
                    loggedIn = true;
                } else if (state == LOGIN_SERVER_TRANSITION) {
                    try (PreparedStatement pps = con.prepareStatement("UPDATE accounts SET loggedin = 0 WHERE id = ?")) {
                        pps.setInt(1, getAccID());
                        pps.executeUpdate();
                    }
                } else {
                    loggedIn = false;
                }
                return state;
            }
        } catch (SQLException e) {
            loggedIn = false;
            e.printStackTrace();
            throw new RuntimeException("login state");
        }
    }

    public boolean checkBirthDate(Calendar date) {
        return date.get(Calendar.YEAR) == birthday.get(Calendar.YEAR) && date.get(Calendar.MONTH) == birthday.get(Calendar.MONTH) && date.get(Calendar.DAY_OF_MONTH) == birthday.get(Calendar.DAY_OF_MONTH);
    }

    private void removePlayer() {
        try {
            player.cancelAllBuffs(true);
            player.cancelAllDebuffs();
            final MaplePlayerShop mps = player.getPlayerShop();
            if (mps != null) {
                mps.removeVisitors();
                player.setPlayerShop(null);
            }
            final HiredMerchant merchant = player.getHiredMerchant();
            if (merchant != null) {
                if (merchant.isOwner(player)) {
                    merchant.setOpen(true);
                } else {
                    merchant.removeVisitor(player);
                }
                try {
                    merchant.saveItems(false);
                } catch (SQLException ex) {
                    System.out.println("Error while saving Hired Merchant items.");
                }
            }
            player.setMessenger(null);
            final MapleMiniGame game = player.getMiniGame();
            if (game != null) {
                player.setMiniGame(null);
                if (game.isOwner(player)) {
                    player.getMap().broadcastMessage(MaplePacketCreator.removeCharBox(player));
                    game.broadcastToVisitor(MaplePacketCreator.getMiniGameClose());
                } else {
                    game.removeVisitor(player);
                }
            }
            NPCScriptManager.dispose(this);
            QuestScriptManager.dispose(this);
            if (player.getTrade() != null) {
                MapleTrade.cancelTrade(player);
            }
            if (player.getEventInstance() != null) {
                player.getEventInstance().playerDisconnected(player);
            }
            if (player.getMap() != null) {
                player.getMap().removePlayer(player);
            }
            if (player.getFakePlayer() != null) {
                player.getMap().removeFakePlayer(player.getFakePlayer());
            }
        } catch (final Throwable t) {
            t.printStackTrace();
        }
    }

    public final void disconnect(boolean shutdown) {
        if (disconnecting) {
            return;
        }
        disconnecting = true;
        if (player != null && player.getClient() != null) {
            player.getGenericEvents().forEach(e -> e.onPlayerDisconnect(player));
            MapleMap map = player.getMap();
            final MapleParty party = player.getParty();
            final int idz = player.getId();
            final int messengerid = player.getMessenger() == null ? 0 : player.getMessenger().getId();
            //final int fid = player.getFamilyId();
            final BuddyList bl = player.getBuddylist();
            final MaplePartyCharacter chrp = new MaplePartyCharacter(player);
            final MapleMessengerCharacter chrm = new MapleMessengerCharacter(player, 0);
            final MapleGuildCharacter chrg = player.getMGC();
            final MapleGuild guild = player.getGuild();

            removePlayer();
            player.saveCooldowns();
            player.saveToDB();
            if (channel == -1 || shutdown) {
                player = null;
                return;
            }
            final MapleWorld worlda = getWorldServer();
            try {
                if (player.getCashShop().isOpened()) {
                    if (!this.serverTransition) { // meaning not changing channels
                        //region messenger
                        if (messengerid > 0) {
                            worlda.leaveMessenger(messengerid, chrm);
                        }
                        //endregion
                        //region custom quests
                        for (MapleQuestStatus status : player.getStartedQuests()) { //This is for those quests that you have to stay logged in for a certain amount of time
                            MapleQuest quest = status.getQuest();
                            if (quest.getTimeLimit() > 0) {
                                MapleQuestStatus newStatus = new MapleQuestStatus(quest, MapleQuestStatus.Status.NOT_STARTED);
                                newStatus.setForfeited(player.getQuest(quest).getForfeited() + 1);
                                player.updateQuest(newStatus);
                            }
                        }
                        //endregion
                        //region guild
                        if (guild != null) {
                            Server.setGuildMemberOnline(chrg, false, player.getClient().getChannel());
                            player.getClient().announce(MaplePacketCreator.showGuildInfo(player));
                        }
                        //endregion
                        //region party
                        if (party != null) {
                            chrp.setOnline(false);
                            worlda.updateParty(party.getId(), PartyOperation.LOG_ONOFF, chrp);
                            if (map != null && party.getLeader().getId() == idz) {
                                MaplePartyCharacter lchr = null;
                                for (MaplePartyCharacter pchr : party.getMembers()) {
                                    if (pchr != null && map.getCharacterById(pchr.getId()) != null && (lchr == null || lchr.getLevel() <= pchr.getLevel())) {
                                        lchr = pchr;
                                    }
                                }
                                if (lchr != null && lchr.getId() != player.getId()) {
                                    worlda.updateParty(party.getId(), PartyOperation.CHANGE_LEADER, lchr);
                                }
                            }
                        }
                        //endregion
                        //region buddy list
                        if (bl != null) {
                            worlda.loggedOff(player.getName(), player.getId(), channel, player.getBuddylist().getBuddyIds());
                        }
                        //endregion
                    }
                } else {
                    if (!this.serverTransition) { // if dc inside of cash shop.
                        //region party
                        if (party != null) {
                            chrp.setOnline(false);
                            worlda.updateParty(party.getId(), PartyOperation.LOG_ONOFF, chrp);
                            if (map != null && party.getLeader().getId() == idz) {
                                MaplePartyCharacter lchr = null;
                                for (MaplePartyCharacter pchr : party.getMembers()) {
                                    if (pchr != null && map.getCharacterById(pchr.getId()) != null && (lchr == null || lchr.getLevel() <= pchr.getLevel())) {
                                        lchr = pchr;
                                    }
                                }
                                if (lchr != null && lchr.getId() != player.getId()) {
                                    worlda.updateParty(party.getId(), PartyOperation.CHANGE_LEADER, lchr);
                                }
                            }
                        }
                        //endregion
                        //region buddy list
                        if (bl != null) {
                            worlda.loggedOff(player.getName(), player.getId(), channel, player.getBuddylist().getBuddyIds());
                        }
                        //endregion
                    }
                }
            } finally {
                LOGGER.info("Player {} disconnected", player.getName());
                getChannelServer().removePlayer(player);
                if (!this.serverTransition) {
                    worlda.removePlayer(player);
                    if (player != null) {
                        player.empty(false);
                    }
                }
                player = null;
            }
        }
        if (!serverTransition && isLoggedIn()) {
            updateLoginState(MapleClient.LOGIN_NOTLOGGEDIN);
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
            player.getClient().disconnect(false);
            disconnect(false);
            return false; //DC both and return, fuck that
        }
        try (Connection con = Server.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT id, guildid, guildrank, name, allianceRank FROM characters WHERE id = ? AND accountid = ?")) {
                ps.setInt(1, cid);
                ps.setInt(2, accId);
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
                ps.setInt(1, accId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        if (rs.getByte("tos") == 1) {
                            return true;
                        }
                    }
                }
            }
            try (PreparedStatement ps = con.prepareStatement("UPDATE accounts SET tos = 1 WHERE id = ?")) {
                ps.setInt(1, accId);
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
            ps.setInt(1, accId);
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
            ps.setInt(2, accId);
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
                ps.setInt(2, accId);
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
            ps.setInt(1, accId);
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
            ps.setInt(2, accId);
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
            disconnect(false);
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
        player.getClient().updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION);
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
            ps.setInt(1, accId);
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
            ps.setInt(2, accId);
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
