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

import java.sql.*;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MapleClient implements Disposable {

    public static final AttributeKey<MapleClient> CLIENT_KEY = AttributeKey.newInstance(MapleClient.class.getName());
    private static final Logger LOGGER = LoggerFactory.getLogger(MapleClient.class);
    private final Lock mutex = new ReentrantLock(true);
    private AESCipher send;
    private AESCipher receive;
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
    private String lastKnownIP;
    private String accountName;

    private Timestamp temporaryBanLength;
    private String banReason;

    private Set<String> hwids;
    private Set<String> macs;

    private long discordId;
    private String discordKey;

    public MapleClient(AESCipher send, AESCipher receive, Channel session) {
        this.send = send;
        this.receive = receive;
        this.session = session;

        gender = 10;
        voteTime = -1;
        characterSlots = 3;
        loginState = LoginState.LogOut;

        hwids = new HashSet<>(3);
        macs = new HashSet<>(3);
    }

    public AESCipher getSendCrypto() {
        return send;
    }

    public AESCipher getReceiveCrypto() {
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
     *
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

    public List<MapleCharacter> loadCharacters() {
        try (Connection con = Server.getConnection()) {
            List<MapleCharacter> ret = new ArrayList<>();
            for (Pair<Integer, String> pair : getCharacterIdentifiers()) {
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

    public List<Pair<Integer, String>> getCharacterIdentifiers() {
        List<Pair<Integer, String>> ret = new ArrayList<>();
        try (Connection con = Server.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT id, name FROM characters WHERE accountid = ?")) {
            ps.setInt(1, getAccID());
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
        if (loginAttempts > 5) {
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
                            banReason = rs.getString("ban_reason");
                            temporaryBanLength = rs.getTimestamp("temporary_ban");
                            return 3;
                        }
                        ID = rs.getInt("id");
                        gmlevel = rs.getInt("gm");
                        pin = rs.getString("pin");
                        pic = rs.getString("pic");
                        gender = rs.getByte("gender");
                        characterSlots = rs.getByte("characterslots");
                        lastKnownIP = rs.getString("last_known_ip");
                        String cryptoPassword = rs.getString("password");

                        if (BCrypt.verifyer().verify(password.getBytes(), cryptoPassword.getBytes()).verified) {
                            loginResponse = 0;
                        } else if (password.equals(cryptoPassword)
                                || StringUtil.checkHash(cryptoPassword, "SHA-1", password)) {
                            loginResponse = 0;
                            try (PreparedStatement fuck = con.prepareStatement("update accounts set password = ? where id = ?")) {
                                String bcryptPassword = BCrypt.with(BCrypt.Version.VERSION_2Y).hashToString(10, password.toCharArray());
                                fuck.setString(1, bcryptPassword);
                                fuck.setInt(2, ID);
                                fuck.executeUpdate();
                            }
                        } else {
                            loginResponse = 4;
                        }
                    }
                }
            }
            if (loginResponse == 0) {
                try (PreparedStatement ps = con.prepareStatement("select mac from accounts_mac where account_id = ?")) {
                    ps.setInt(1, getAccID());
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            macs.add(rs.getString("mac"));
                        }
                    }
                }
                try (PreparedStatement ps = con.prepareStatement("select hwid from accounts_hwid where account_id = ?")) {
                    ps.setInt(1, getAccID());
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            hwids.add(rs.getString("hwid"));
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (loginResponse != 0) {
            loginAttempts++;
        }
        return loginResponse;
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
            hwids.add(hwid.toString());

            try (Connection con = Server.getConnection()) {
                Database.executeSingle(con, "delete from accounts_hwid where account_id = ?", getAccID());
                try (PreparedStatement ps = con.prepareStatement("insert into accounts_hwid values (?, ?)")) {
                    ps.setInt(1, getAccID());
                    for (String s : hwids) {
                        ps.setString(2, s);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            disconnect(); // Invalid HWID...
        }
    }

    public void updateMacs(String macData) {
        macs.addAll(Arrays.asList(macData.split(", ")));
        try (Connection con = Server.getConnection()) {
            Database.executeSingle(con, "delete from accounts_mac where account_id = ?", getAccID());
            try (PreparedStatement ps = con.prepareStatement("insert into accounts_mac values (?, ?)")) {
                ps.setInt(1, getAccID());
                for (String s : macs) {
                    ps.setString(2, s);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
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

    public LoginState getLoginState() {
        return loginState;
    }

    public void setLoginState(LoginState loginState) {
        this.loginState = loginState;
    }


    public void updateLoginState(LoginState loginState) {
        setLoginState(loginState);
        try (Connection con = Server.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("update accounts set last_known_ip = ?, loggedin = ?, last_login = current_timestamp() where id = ?")) {
                ps.setString(1, getLastKnownIP());
                ps.setInt(2, loginState.ordinal());
                ps.setInt(3, getAccID());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to update login state for account {}", this, e);
        }
    }


    public LoginState checkLoginState() {
        if (getAccID() == 0) return LoginState.LogOut;
        try (Connection con = Server.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("select loggedin, last_login, UNIX_TIMESTAMP(birthday) as birthday from accounts where id = ?")) {
                ps.setInt(1, getAccID());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new NullPointerException();
                    }
                    birthday = Calendar.getInstance();
                    long unixBirthday = rs.getLong("birthday");
                    if (unixBirthday > 0) {
                        birthday.setTimeInMillis(unixBirthday * 1000);
                    }
                    LoginState state = LoginState.values()[rs.getInt("loggedin")];
                    switch (state) {
                        case LogOut:
                        case Transfer:
                            // typically check how long the account has been in this state
                            // and apply an "account-fix" where we change the login state
                            break;
                        case Login:
                            if (player == null) {
                                updateLoginState(LoginState.LogOut);
                            }
                            break;
                    }
                    return state;
                }
            }
        } catch (SQLException | NullPointerException e) {
            LOGGER.error("unable to check login state for {}", this.toString(), e);
        }
        // i guess just to be safe; remote hack, anybody?
        return LoginState.LogOut;
    }

    public boolean checkBirthDate(Calendar date) {
        return date.get(Calendar.YEAR) == birthday.get(Calendar.YEAR) && date.get(Calendar.MONTH) == birthday.get(Calendar.MONTH) && date.get(Calendar.DAY_OF_MONTH) == birthday.get(Calendar.DAY_OF_MONTH);
    }

    @Override
    public void dispose() {
        // prevent any kind of fuck shit exploits pls
        session.attr(CLIENT_KEY).set(null);
        session.close();
        ID = 0;
        player = null;

        NPCScriptManager.dispose(this);
        QuestScriptManager.dispose(this);
    }

    @Override
    public String toString() {
        return "MapleClient{" +
                "ID=" + ID +
                ", gmlevel=" + gmlevel +
                ", loginState=" + loginState +
                ", lastKnownIP='" + lastKnownIP + '\'' +
                ", accountName='" + accountName + '\'' +
                ", discordId=" + discordId +
                '}';
    }

    public final void disconnect() {
        if (disconnecting) {
            return;
        }
        disconnecting = true;
        final MapleWorld world = getWorldServer();
        final MapleCharacter player = getPlayer();
        try {
            if (player != null) {
                player.getMap().removePlayer(player);
                getWorldServer().getPlayerStorage().remove(player.getId());
                player.getGenericEvents().forEach(e -> e.onPlayerDisconnect(player));
                player.getGenericEvents().clear();

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
                    MaplePartyCharacter member = party.get(player.getId());
                    member.updateWithPlayer(player);
                    if (getLoginState() != LoginState.Transfer) {
                        member.setChannelID(-2);
                        member.setPlayer(null);
                        if (party.getLeaderPlayerID() == player.getId()) {
                            var newLeader = party.values().stream().filter(p -> p.getPlayer() != null).findAny();
                            newLeader.ifPresent(m -> party.sendPacket(MaplePacketCreator.updateParty(m.getChannelID(), party, PartyOperation.CHANGE_LEADER, m)));
                        }
                        party.sendPacket(MaplePacketCreator.updateParty(getChannel(), party, PartyOperation.LOG_ONOFF, member));
                    }
                }
                BuddyList friends = player.getBuddylist();
                if (getLoginState() == LoginState.Login) {
                    world.loggedOff(player.getName(), player.getId(), channelID, friends.getBuddyIds());
                } else if (getLoginState() == LoginState.Transfer) {
                    world.loggedOn(player.getName(), player.getId(), channelID, friends.getBuddyIds());
                }
            }
        } catch (Throwable t) {
            // whatever happens here, we still want to save the player.
            t.printStackTrace();
        } finally {
            LoginState loginState = checkLoginState();
            if (loginState != LoginState.Transfer) {
                if (loginState == LoginState.Login) {
                    updateLoginState(LoginState.LogOut);
                }
                Functions.requireNotNull(player, MapleCharacter::dispose);
            }
            dispose();
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

    public Set<String> getHardwareIDs() {
        return hwids;
    }

    public Set<String> getMacs() {
        return Collections.unmodifiableSet(macs);
    }

    public Timestamp getTemporaryBanLength() {
        return temporaryBanLength;
    }

    public String getBanReason() {
        return banReason;
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

    public boolean changeChannel(int channel) {
        if (player.isBanned()) {
            disconnect();
            return false;
        }
        if (!player.isAlive() || FieldLimit.CHANGECHANNEL.check(player.getMap().getFieldLimit())) {
            announce(MaplePacketCreator.enableActions());
            return false;
        }

        MapleChannel cserv = getWorldServer().getChannel(channel);
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
        getWorldServer().getPlayerStorage().remove(player.getId());
        updateLoginState(LoginState.Transfer);
        announce(MaplePacketCreator.getChannelChange(cserv.getNetworkAddress(), cserv.getPort()));
        return true;
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
