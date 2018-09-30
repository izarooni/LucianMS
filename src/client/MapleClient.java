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
package client;

import client.inventory.MapleInventoryType;
import com.lucianms.io.scripting.npc.NPCConversationManager;
import com.lucianms.io.scripting.npc.NPCScriptManager;
import com.lucianms.io.scripting.quest.QuestActionManager;
import com.lucianms.io.scripting.quest.QuestScriptManager;
import com.lucianms.scheduler.Task;
import com.lucianms.scheduler.TaskExecutor;
import net.server.Server;
import net.server.channel.Channel;
import net.server.guild.MapleGuild;
import net.server.guild.MapleGuildCharacter;
import net.server.world.*;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.MapleMiniGame;
import server.MaplePlayerShop;
import server.MapleTrade;
import server.maps.FieldLimit;
import server.maps.HiredMerchant;
import server.maps.MapleMap;
import server.quest.MapleQuest;
import tools.*;

import javax.script.ScriptEngine;
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
import java.util.stream.Collectors;

public class MapleClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(MapleClient.class);

    public static final int LOGIN_NOTLOGGEDIN = 0;
    public static final int LOGIN_SERVER_TRANSITION = 1;
    public static final int LOGIN_LOGGEDIN = 2;
    public static final String CLIENT_KEY = "CLIENT";

    private final Lock mutex = new ReentrantLock(true);
    private MapleAESOFB send;
    private MapleAESOFB receive;
    private IoSession session;
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
    private Map<String, ScriptEngine> engines = new HashMap<>();
    private Task idleTask = null;
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

    private long discordId = 0;
    private String discordKey = null;

    public MapleClient(MapleAESOFB send, MapleAESOFB receive, IoSession session) {
        this.send = send;
        this.receive = receive;
        this.session = session;
    }

    public static long dottedQuadToLong(String dottedQuad) throws RuntimeException {
        String[] quads = dottedQuad.split("\\.");
        if (quads.length != 4) {
            throw new RuntimeException("Invalid IP Address format.");
        }
        long ipAddress = 0;
        for (int i = 0; i < 4; i++) {
            int quad = Integer.parseInt(quads[i]);
            ipAddress += (long) (quad % 256) * (long) Math.pow(256, (double) (4 - i));
        }
        return ipAddress;
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

    public synchronized MapleAESOFB getReceiveCrypto() {
        return receive;
    }

    public synchronized MapleAESOFB getSendCrypto() {
        return send;
    }

    public synchronized IoSession getSession() {
        return session;
    }

    public MapleCharacter getPlayer() {
        return player;
    }

    public void setPlayer(MapleCharacter player) {
        this.player = player;
    }

    public void sendCharList(int server) {
        this.session.write(MaplePacketCreator.getCharList(this, server));
    }

    /**
     * Looks for a player in the database with an ID that matches the account ID of this client
     *
     * @param playerId ID of a character
     * @return true if the account the specified character belongs to this account, false otherwise
     */
    public boolean playerBelongs(int playerId) {
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT count(*) AS total FROM characters WHERE id = ? AND accountid = ? ")) {
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
        try {
            List<MapleCharacter> ret = new ArrayList<>();
            for (Pair<Integer, String> pair : loadCharactersInternal(serverId)) {
                MapleCharacter load = MapleCharacter.loadCharFromDB(pair.getLeft(), this, false);
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

    public List<String> loadCharacterNames(int serverId) {
        return loadCharactersInternal(serverId).stream().map(Pair::getRight).collect(Collectors.toList());
    }

    private List<Pair<Integer, String>> loadCharactersInternal(int serverId) {
        List<Pair<Integer, String>> ret = new ArrayList<>();
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT id, name FROM characters WHERE accountid = ? AND world = ?")) {
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
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT COUNT(*) FROM ipbans WHERE ? LIKE CONCAT(ip, '%')")) {
                ps.setString(1, session.getRemoteAddress().toString());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    if (rs.getInt(1) > 0) {
                        ret = true;
                    }
                }
            }
        } catch (SQLException ignored) {
        }
        return ret;
    }

    public int getVoteTime() {
        if (voteTime != -1) {
            return voteTime;
        }
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT date FROM bit_votingrecords WHERE UPPER(account) = UPPER(?)")) {
                ps.setString(1, accountName);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return -1;
                    }
                    voteTime = rs.getInt("date");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
        return voteTime;
    }

    public void resetVoteTime() {
        voteTime = -1;
    }

    public boolean hasVotedAlready() {
        Date currentDate = new Date();
        int timeNow = (int) (currentDate.getTime() / 1000);
        int difference = (timeNow - getVoteTime());
        return difference < 86400 && difference > 0;
    }

    public boolean hasBannedHWID() {
        if (hwid == null) {
            return false;
        }

        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT COUNT(*) FROM hwidbans WHERE hwid LIKE ?")) {
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
        try {
            StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM macbans WHERE mac IN (");
            for (i = 0; i < macs.size(); i++) {
                sql.append("?");
                if (i != macs.size() - 1) {
                    sql.append(", ");
                }
            }
            sql.append(")");
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql.toString())) {
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
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    private void loadHWIDIfNescessary() throws SQLException {
        if (hwid == null) {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT hwid FROM accounts WHERE id = ?")) {
                ps.setInt(1, accId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        hwid = rs.getString("hwid");
                    }
                }
            }
        }
    }

    private void loadMacsIfNescessary() throws SQLException {
        if (macs.isEmpty()) {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT macs FROM accounts WHERE id = ?")) {
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
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("INSERT INTO hwidbans (hwid) VALUES (?)")) {
            loadHWIDIfNescessary();
            ps.setString(1, hwid);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void banMacs() {
        Connection con = DatabaseConnection.getConnection();
        try {
            loadMacsIfNescessary();
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

    public int finishLogin() {
        synchronized (MapleClient.class) {
            if (getLoginState() > LOGIN_NOTLOGGEDIN) { // 0 = LOGIN_NOTLOGGEDIN, 1= LOGIN_SERVER_TRANSITION, 2 = LOGIN_LOGGEDIN
                loggedIn = false;
                return 7;
            }
            updateLoginState(LOGIN_LOGGEDIN);
        }
        return 0;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE accounts SET pin = ? WHERE id = ?")) {
                ps.setString(1, pin);
                ps.setInt(2, accId);
                ps.executeUpdate();
            }
        } catch (SQLException ignored) {
        }
    }

    public boolean checkPin(String other) {
        pinattempt++;
        if (pinattempt > 5) {
            getSession().closeNow();
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
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE accounts SET pic = ? WHERE id = ?")) {
                ps.setString(1, pic);
                ps.setInt(2, accId);
                ps.executeUpdate();
            }
        } catch (SQLException ignored) {
        }
    }

    public boolean checkPic(String other) {
        picattempt++;
        if (picattempt > 5) {
            getSession().closeNow();
        }
        if (pic.equals(other)) {
            picattempt = 0;
            return true;
        }
        return false;
    }

    public int login(String login, String pwd) {
        loginattempt++;
        int loginok = 5;
        Connection con = DatabaseConnection.getConnection();
        try {
            try (PreparedStatement ps = con.prepareStatement("SELECT id, password, salt, gender, banned, gm, pin, pic, characterslots, tos FROM accounts WHERE name = ?")) {
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
                        String passhash = rs.getString("password");
                        String salt = rs.getString("salt");
                        byte tos = rs.getByte("tos");
                        if (getLoginState() > LOGIN_NOTLOGGEDIN) { // already loggedin
                            loggedIn = false;
                            loginok = 7;
                        } else if (pwd.equals(passhash) || checkHash(passhash, "SHA-1", pwd) || checkHash(passhash, "SHA-512", pwd + salt)) {
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
                ps.setString(2, session.getRemoteAddress().toString());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (loginok == 0) {
            loginattempt = 0;
        } else if (loginattempt > 4) {
            getSession().closeNow();
        }
        return loginok;
    }

    public Calendar getTempBanCalendar() {
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps = null;
        ResultSet rs = null;
        final Calendar lTempban = Calendar.getInstance();
        try {
            ps = con.prepareStatement("SELECT `tempban` FROM accounts WHERE id = ?");
            ps.setInt(1, getAccID());
            rs = ps.executeQuery();
            if (!rs.next()) {
                return null;
            }
            long blubb = rs.getLong("tempban");
            if (blubb == 0) { // basically if timestamp in db is 0000-00-00
                return null;
            }
            lTempban.setTimeInMillis(rs.getTimestamp("tempban").getTime());
            return lTempban;
        } catch (SQLException ignored) {
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException ignored) {
            }
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

            PreparedStatement ps = null;
            try {
                ps = DatabaseConnection.getConnection().prepareStatement("UPDATE accounts SET hwid = ? WHERE id = ?");
                ps.setString(1, this.hwid);
                ps.setInt(2, accId);
                ps.executeUpdate();
                ps.close();
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (ps != null && !ps.isClosed()) {
                        ps.close();
                    }
                } catch (SQLException ignored) {
                }
            }
        } else {
            disconnect(false, false); // Invalid HWID...
        }
    }

    public void updateMacs(String macData) {
        macs.addAll(Arrays.asList(macData.split(", ")));
        StringBuilder newMacData = new StringBuilder();
        Iterator<String> iter = macs.iterator();
        PreparedStatement ps = null;
        while (iter.hasNext()) {
            String cur = iter.next();
            newMacData.append(cur);
            if (iter.hasNext()) {
                newMacData.append(", ");
            }
        }
        try {
            ps = DatabaseConnection.getConnection().prepareStatement("UPDATE accounts SET macs = ? WHERE id = ?");
            ps.setString(1, newMacData.toString());
            ps.setInt(2, accId);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (ps != null && !ps.isClosed()) {
                    ps.close();
                }
            } catch (SQLException ignored) {
            }
        }
    }

    public int getAccID() {
        return accId;
    }

    public void setAccID(int id) {
        this.accId = id;
    }

    public void updateLoginState(int newstate) {
        try {
            Connection con = DatabaseConnection.getConnection();
            try (PreparedStatement ps = con.prepareStatement("UPDATE accounts SET loggedin = ?, lastlogin = CURRENT_TIMESTAMP() WHERE id = ?")) {
                ps.setInt(1, newstate);
                ps.setInt(2, getAccID());
                ps.executeUpdate();
            }
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
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT loggedin, lastlogin, UNIX_TIMESTAMP(birthday) AS birthday FROM accounts WHERE id = ?");
            ps.setInt(1, getAccID());
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                throw new RuntimeException("getLoginState - MapleClient");
            }
            birthday = Calendar.getInstance();
            long blubb = rs.getLong("birthday");
            if (blubb > 0) {
                birthday.setTimeInMillis(blubb * 1000);
            }
            int state = rs.getInt("loggedin");
            if (state == LOGIN_SERVER_TRANSITION) {
                if (rs.getTimestamp("lastlogin").getTime() + 30000 < System.currentTimeMillis()) {
                    state = LOGIN_NOTLOGGEDIN;
                    updateLoginState(LOGIN_NOTLOGGEDIN);
                }
            } else if (state == LOGIN_LOGGEDIN && player == null) {
                state = LOGIN_LOGGEDIN;
                updateLoginState(LOGIN_LOGGEDIN);
            }
            rs.close();
            ps.close();
            if (state == LOGIN_LOGGEDIN) {
                loggedIn = true;
            } else if (state == LOGIN_SERVER_TRANSITION) {
                ps = con.prepareStatement("UPDATE accounts SET loggedin = 0 WHERE id = ?");
                ps.setInt(1, getAccID());
                ps.executeUpdate();
                ps.close();
            } else {
                loggedIn = false;
            }
            return state;
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

    public final void disconnect(boolean shutdown, boolean cashshop) {//once per MapleClient instance
        if (disconnecting) {
            return;
        }
        disconnecting = true;
        if (player != null && player.isLoggedin() && player.getClient() != null) {
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
            final World worlda = getWorldServer();
            try {
                if (!cashshop) {
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
                            final Server server = Server.getInstance();
                            server.setGuildMemberOnline(chrg, false, player.getClient().getChannel());
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
            } catch (final Exception e) {
                e.printStackTrace();
            } finally {
                LOGGER.info("Player {} logged-out", player.getName());
                getChannelServer().removePlayer(player);
                if (!this.serverTransition) {
                    worlda.removePlayer(player);
                    if (player != null) {//no idea, occur :(
                        player.empty(false);
                    }
                    player.logOff();
                }
                player = null;
            }
        }
        if (!serverTransition && isLoggedIn()) {
            updateLoginState(MapleClient.LOGIN_NOTLOGGEDIN);
            session.removeAttribute(MapleClient.CLIENT_KEY); // prevents double dcing during login
            session.closeNow(); // instead of using a deprecated method
        }
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public Channel getChannelServer() {
        return Server.getInstance().getChannel(world, channel);
    }

    public World getWorldServer() {
        return Server.getInstance().getWorld(world);
    }

    public Channel getChannelServer(byte channel) {
        return Server.getInstance().getChannel(world, channel);
    }

    public boolean deleteCharacter(int cid) {
        Connection con = DatabaseConnection.getConnection();

        MapleCharacter player = Server.getInstance().getWorld(0).getPlayerStorage().getCharacterById(cid);
        if (player != null) {
            player.getClient().disconnect(false, false);
            disconnect(false, false);
            return false; //DC both and return, fuck that
        }
        try {
            try (PreparedStatement ps = con.prepareStatement("SELECT id, guildid, guildrank, name, allianceRank FROM characters WHERE id = ? AND accountid = ?")) {
                ps.setInt(1, cid);
                ps.setInt(2, accId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return false;
                    }
                    if (rs.getInt("guildid") > 0) {
                        try {
                            Server.getInstance().deleteGuildCharacter(new MapleGuildCharacter(cid, 0, rs.getString("name"), (byte) -1, (byte) -1, 0, rs.getInt("guildrank"), rs.getInt("guildid"), false, rs.getInt("allianceRank")));
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

    public void setAccountName(String a) {
        this.accountName = a;
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
                try {
                    if (lastPong < then) {
                        if (getSession() != null && getSession().isConnected()) {
                            getSession().closeNow();
                        }
                    }
                } catch (NullPointerException ignored) {
                }
            }
        }, 30000);
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

    public void setEngine(String name, ScriptEngine e) {
        engines.put(name, e);
    }

    public ScriptEngine getEngine(String name) {
        return engines.get(name);
    }

    public void removeEngine(String name) {
        engines.remove(name);
    }

    public Task getIdleTask() {
        return idleTask;
    }

    public void setIdleTask(Task idleTask) {
        this.idleTask = idleTask;
    }

    public NPCConversationManager getCM() {
        return NPCScriptManager.getConversationManager(this);
    }

    public QuestActionManager getQM() {
        return QuestScriptManager.getActionManager(this);
    }

    public boolean acceptToS() {
        boolean disconnectForBeingAFaggot = false;
        if (accountName == null) {
            return true;
        }
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT `tos` FROM accounts WHERE id = ?")) {
                ps.setInt(1, accId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        if (rs.getByte("tos") == 1) {
                            disconnectForBeingAFaggot = true;
                        }
                    }
                }
            }
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE accounts SET tos = 1 WHERE id = ?")) {
                ps.setInt(1, accId);
                ps.executeUpdate();
            }
        } catch (SQLException ignored) {
        }
        return disconnectForBeingAFaggot;
    }

    public int getVotePoints() {
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT votepoints FROM accounts WHERE id = ?")) {
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
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE accounts SET votepoints = ? WHERE id = ?")) {
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
            Connection con = DatabaseConnection.getConnection();
            try {
                try (PreparedStatement ps = con.prepareStatement("UPDATE accounts SET characterslots = ? WHERE id = ?")) {
                    ps.setInt(1, this.characterSlots += 1);
                    ps.setInt(2, accId);
                    ps.executeUpdate();
                }
            } catch (SQLException ignored) {
            }
            return true;
        }
        return false;
    }

    public final byte getGReason() {
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT `greason` FROM `accounts` WHERE id = ?")) {
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
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE accounts SET gender = ? WHERE id = ?")) {
            ps.setByte(1, gender);
            ps.setInt(2, accId);
            ps.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

    public synchronized void announce(final byte[] packet) {//MINA CORE IS A FUCKING BITCH AND I HATE IT <3
        session.write(packet);
    }

    public void changeChannel(int channel) {
        Server server = Server.getInstance();
        if (player.isBanned()) {
            disconnect(false, false);
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
        server.getPlayerBuffStorage().addBuffsToStorage(player.getId(), player.getAllBuffs());
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
        player.saveToDB();
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
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT donationpoints FROM accounts WHERE id = ?")) {
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
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE accounts SET donationpoints = ? WHERE id = ?")) {
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
