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
package com.lucianms.server.guild;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleClient;
import com.lucianms.server.Server;
import com.lucianms.server.channel.MapleChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.MaplePacketCreator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class MapleGuild {

    private static final Logger LOGGER = LoggerFactory.getLogger(MapleGuild.class);
    public final static int CREATE_GUILD_COST = 1000000;
    public final static int CHANGE_EMBLEM_COST = 1500000;

    private enum BCOp {
        NONE, DISBAND, EMBELMCHANGE
    }

    private List<MapleGuildCharacter> members = new ArrayList<>(30);
    private String rankTitles[] = new String[5]; // 1 = master, 2 = jr, 5 = lowest member
    private String name, notice;
    private int id, gp, logo, logoColor, leader, capacity, logoBG, logoBGColor, signature, allianceId;
    private int world;
    private Map<Integer, List<Integer>> notifications = new LinkedHashMap<>();
    private boolean bDirty = true;


    public MapleGuild(int guildid, int world) {
        this.world = world;
        try (Connection con = Server.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM guilds WHERE guildid = " + guildid)) {
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.first()) {
                        id = -1;
                        return;
                    }
                    id = guildid;
                    name = rs.getString("name");
                    gp = rs.getInt("GP");
                    logo = rs.getInt("logo");
                    logoColor = rs.getInt("logoColor");
                    logoBG = rs.getInt("logoBG");
                    logoBGColor = rs.getInt("logoBGColor");
                    capacity = rs.getInt("capacity");
                    for (int i = 1; i <= 5; i++) {
                        rankTitles[i - 1] = rs.getString("rank" + i + "title");
                    }
                    leader = rs.getInt("leader");
                    notice = rs.getString("notice");
                    signature = rs.getInt("signature");
                    allianceId = rs.getInt("allianceId");
                }
            }
            try (PreparedStatement ps = con.prepareStatement("SELECT id, name, level, job, guildrank, allianceRank FROM characters WHERE guildid = ? ORDER BY guildrank ASC, name ASC")) {
                ps.setInt(1, guildid);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        members.add(new MapleGuildCharacter(rs.getInt("id"),
                                rs.getInt("level"),
                                rs.getString("name"),
                                (byte) -1, world,
                                rs.getInt("job"),
                                rs.getInt("guildrank"),
                                guildid, false,
                                rs.getInt("allianceRank")));
                    }
                }
            }
        } catch (SQLException se) {

            System.out.println("unable to read guild information from sql" + se);
        }
    }

    public void buildNotifications() {
        if (!bDirty) {
            return;
        }
        Set<Integer> chs = Server.getInstance().getChannelServer(world);
        if (notifications.keySet().size() != chs.size()) {
            notifications.clear();
            for (Integer ch : chs) {
                notifications.put(ch, new LinkedList<Integer>());
            }
        } else {
            for (List<Integer> l : notifications.values()) {
                l.clear();
            }
        }
        for (MapleGuildCharacter mgc : members) {
            if (!mgc.isOnline()) {
                continue;
            }
            List<Integer> ch = notifications.get(mgc.getChannel());
            if (ch != null) ch.add(mgc.getId());
            //Unable to connect to Channel... error was here
        }
        bDirty = false;
    }

    public void writeToDB(boolean bDisband) {
        try (Connection con = Server.getConnection()) {
            if (!bDisband) {
                StringBuilder builder = new StringBuilder();
                builder.append("UPDATE guilds SET GP = ?, logo = ?, logoColor = ?, logoBG = ?, logoBGColor = ?, ");
                for (int i = 0; i < 5; i++) {
                    builder.append("rank").append(i + 1).append("title = ?, ");
                }
                builder.append("capacity = ?, notice = ? WHERE guildid = ?");
                try (PreparedStatement ps = con.prepareStatement(builder.toString())) {
                    ps.setInt(1, gp);
                    ps.setInt(2, logo);
                    ps.setInt(3, logoColor);
                    ps.setInt(4, logoBG);
                    ps.setInt(5, logoBGColor);
                    for (int i = 6; i < 11; i++) {
                        ps.setString(i, rankTitles[i - 6]);
                    }
                    ps.setInt(11, capacity);
                    ps.setString(12, notice);
                    ps.setInt(13, this.id);
                    ps.execute();
                }
            } else {
                try (PreparedStatement ps = con.prepareStatement("UPDATE characters SET guildid = 0, guildrank = 5 WHERE guildid = ?")) {
                    ps.setInt(1, this.id);
                    ps.execute();
                }
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM guilds WHERE guildid = ?")) {
                    ps.setInt(1, this.id);
                    ps.execute();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            broadcast(MaplePacketCreator.guildDisband(this.id));
        }
    }

    public int getId() {
        return id;
    }

    public int getLeaderId() {
        return leader;
    }

    public int getGP() {
        return gp;
    }

    public int getLogo() {
        return logo;
    }

    public void setLogo(int l) {
        logo = l;
    }

    public int getLogoColor() {
        return logoColor;
    }

    public void setLogoColor(int c) {
        logoColor = c;
    }

    public int getLogoBG() {
        return logoBG;
    }

    public void setLogoBG(int bg) {
        logoBG = bg;
    }

    public int getLogoBGColor() {
        return logoBGColor;
    }

    public void setLogoBGColor(int c) {
        logoBGColor = c;
    }

    public String getNotice() {
        if (notice == null) {
            return "";
        }
        return notice;
    }

    public String getName() {
        return name;
    }

    public java.util.Collection<MapleGuildCharacter> getMembers() {
        return java.util.Collections.unmodifiableCollection(members);
    }

    public int getCapacity() {
        return capacity;
    }

    public int getSignature() {
        return signature;
    }

    public void broadcast(final byte[] packet) {
        broadcast(packet, -1, BCOp.NONE);
    }

    public void broadcast(final byte[] packet, int exception) {
        broadcast(packet, exception, BCOp.NONE);
    }

    public void broadcast(final byte[] packet, int exceptionId, BCOp bcop) {
        synchronized (notifications) {
            if (bDirty) {
                buildNotifications();
            }
            try {
                for (Integer b : Server.getInstance().getChannelServer(world)) {
                    if (notifications.get(b).size() > 0) {
                        if (bcop == BCOp.DISBAND) {
                            Server.getInstance().getWorld(world).setGuildAndRank(notifications.get(b), 0, 5, exceptionId);
                        } else if (bcop == BCOp.EMBELMCHANGE) {
                            Server.getInstance().getWorld(world).changeEmblem(this.id, notifications.get(b), new MapleGuildSummary(this));
                        } else {
                            Server.getInstance().getWorld(world).sendPacket(notifications.get(b), packet, exceptionId);
                        }
                    }
                }
            } catch (Exception re) {
                System.out.println("Failed to contact com.lucianms.server.events.channel(s) for broadcast.");//fu?
            }
        }
    }

    public void guildMessage(final byte[] serverNotice) {
        for (MapleGuildCharacter mgc : members) {
            for (MapleChannel cs : Server.getInstance().getChannelsFromWorld(world)) {
                if (cs.getPlayerStorage().getPlayerByID(mgc.getId()) != null) {
                    cs.getPlayerStorage().getPlayerByID(mgc.getId()).getClient().announce(serverNotice);
                    break;
                }
            }
        }
    }

    public final void setOnline(int cid, boolean online, int channel) {
        boolean bBroadcast = true;
        for (MapleGuildCharacter mgc : members) {
            if (mgc.getId() == cid) {
                if (mgc.isOnline() && online) {
                    bBroadcast = false;
                }
                mgc.setOnline(online);
                mgc.setChannel(channel);
                break;
            }
        }
        if (bBroadcast) {
            this.broadcast(MaplePacketCreator.guildMemberOnline(id, cid, online), cid);
        }
        bDirty = true;
    }

    public void guildChat(String name, int cid, String message) {
        this.broadcast(MaplePacketCreator.multiChat(name, message, 2), cid);
    }

    public String getRankTitle(int rank) {
        return rankTitles[rank - 1];
    }

    public static int createGuild(int leaderId, String name) {
        try (Connection con = Server.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT guildid FROM guilds WHERE name = ?")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.first()) {
                        return 0;
                    }
                }
            }
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO guilds (`leader`, `name`, `signature`) VALUES (?, ?, ?)")) {
                ps.setInt(1, leaderId);
                ps.setString(2, name);
                ps.setInt(3, (int) System.currentTimeMillis());
                ps.execute();
            }
            try (PreparedStatement ps = con.prepareStatement("SELECT guildid FROM guilds WHERE leader = ?")) {
                ps.setInt(1, leaderId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.first();
                    return rs.getInt("guildid");
                }
            }
        } catch (SQLException e) {
            return 0;
        }
    }

    public int addGuildMember(MapleGuildCharacter mgc) {
        if (members.size() >= capacity) {
            return 0;
        }
        for (int i = members.size() - 1; i >= 0; i--) {
            if (members.get(i).getGuildRank() < 5 || members.get(i).getName().compareTo(mgc.getName()) < 0) {
                members.add(i + 1, mgc);
                bDirty = true;
                break;
            }
        }
        this.broadcast(MaplePacketCreator.newGuildMember(mgc));
        return 1;
    }

    public void leaveGuild(MapleGuildCharacter mgc) {
        this.broadcast(MaplePacketCreator.memberLeft(mgc, false));
        members.remove(mgc);
        bDirty = true;
    }

    public void expelMember(MapleGuildCharacter initiator, String name, int cid) {
        java.util.Iterator<MapleGuildCharacter> itr = members.iterator();
        try (Connection con = Server.getConnection()) {
            while (itr.hasNext()) {
                MapleGuildCharacter mgc = itr.next();
                if (mgc.getId() == cid && initiator.getGuildRank() < mgc.getGuildRank()) {
                    broadcast(MaplePacketCreator.memberLeft(mgc, true));
                    itr.remove();
                    bDirty = true;
                    if (mgc.isOnline()) {
                        Server.getInstance().getWorld(mgc.getWorld()).setGuildAndRank(cid, 0, 5);
                    } else {
                        try (PreparedStatement ps = con.prepareStatement("INSERT INTO notes (`to`, `from`, `message`, `timestamp`) VALUES (?, ?, ?, ?)")) {
                            ps.setString(1, mgc.getName());
                            ps.setString(2, initiator.getName());
                            ps.setString(3, "You have been expelled from the guild.");
                            ps.setLong(4, System.currentTimeMillis());
                            ps.executeUpdate();
                        } catch (SQLException e) {
                            LOGGER.error("Unable to expel member '{}': {}", name, e.getMessage());
                        }
                        Server.getInstance().getWorld(mgc.getWorld()).setOfflineGuildStatus((short) 0, (byte) 5, cid);
                    }
                    return;
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Unable to create database connection: {}", e.getMessage());
        }
    }

    public void changeRank(int cid, int newRank) {
        for (MapleGuildCharacter mgc : members) {
            if (cid == mgc.getId()) {
                try {
                    if (mgc.isOnline()) {
                        Server.getInstance().getWorld(mgc.getWorld()).setGuildAndRank(cid, this.id, newRank);
                    } else {
                        Server.getInstance().getWorld(mgc.getWorld()).setOfflineGuildStatus((short) this.id, (byte) newRank, cid);
                    }
                } catch (Exception re) {
                    re.printStackTrace();
                    return;
                }
                mgc.setGuildRank(newRank);
                this.broadcast(MaplePacketCreator.changeRank(mgc));
                return;
            }
        }
    }

    public void setGuildNotice(String notice) {
        this.notice = notice;
        writeToDB(false);
        this.broadcast(MaplePacketCreator.guildNotice(this.id, notice));
    }

    public void memberLevelJobUpdate(MapleGuildCharacter mgc) {
        for (MapleGuildCharacter member : members) {
            if (mgc.equals(member)) {
                member.setJobId(mgc.getJobId());
                member.setLevel(mgc.getLevel());
                this.broadcast(MaplePacketCreator.guildMemberLevelJobUpdate(mgc));
                break;
            }
        }
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof MapleGuildCharacter)) {
            return false;
        }
        MapleGuildCharacter o = (MapleGuildCharacter) other;
        return (o.getId() == id && o.getName().equals(name));
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 89 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 89 * hash + this.id;
        return hash;
    }

    public void changeRankTitle(String[] ranks) {
        System.arraycopy(ranks, 0, rankTitles, 0, 5);
        this.broadcast(MaplePacketCreator.rankTitleChange(this.id, ranks));
        this.writeToDB(false);
    }

    public void disbandGuild() {
        this.writeToDB(true);
        this.broadcast(null, -1, BCOp.DISBAND);
    }

    public void setGuildEmblem(short bg, byte bgcolor, short logo, byte logocolor) {
        this.logoBG = bg;
        this.logoBGColor = bgcolor;
        this.logo = logo;
        this.logoColor = logocolor;
        this.writeToDB(false);
        this.broadcast(null, -1, BCOp.EMBELMCHANGE);
    }

    public MapleGuildCharacter getMGC(int cid) {
        for (MapleGuildCharacter mgc : members) {
            if (mgc.getId() == cid) {
                return mgc;
            }
        }
        return null;
    }

    public boolean increaseCapacity() {
        if (capacity > 99) {
            return false;
        }
        capacity += 5;
        this.writeToDB(false);
        this.broadcast(MaplePacketCreator.guildCapacityChange(this.id, this.capacity));
        return true;
    }

    public void gainGP(int amount) {
        this.gp += amount;
        this.writeToDB(false);
        this.guildMessage(MaplePacketCreator.updateGP(this.id, this.gp));
        this.guildMessage(MaplePacketCreator.getGPMessage(amount));
    }

    public void removeGP(int amount) {
        this.gp -= amount;
        this.writeToDB(false);
        this.guildMessage(MaplePacketCreator.updateGP(this.id, this.gp));
    }

    public static MapleGuildResponse sendInvite(MapleClient c, String targetName) {
        MapleCharacter mc = c.getChannelServer().getPlayerStorage().getPlayerByName(targetName);
        if (mc == null) {
            return MapleGuildResponse.NOT_IN_CHANNEL;
        }
        if (mc.getGuildId() > 0) {
            return MapleGuildResponse.ALREADY_IN_GUILD;
        }
        mc.getClient().announce(MaplePacketCreator.guildInvite(c.getPlayer().getGuildId(), c.getPlayer().getName()));
        return null;
    }

    public static void displayGuildRanks(MapleClient c, int npcid) {
        try (Connection con = c.getChannelServer().getConnection(); PreparedStatement ps = con.prepareStatement("SELECT `name`, `GP`, `logoBG`, `logoBGColor`, `logo`, `logoColor` FROM guilds WHERE NOT `guildid` = '1' ORDER BY `GP` DESC LIMIT 50")) {
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    c.announce(MaplePacketCreator.showGuildRanks(npcid, rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Unable to obtain guild rankings", e);
        }
    }

    public int getAllianceId() {
        return allianceId;
    }

    public void setAllianceId(int aid) {
        this.allianceId = aid;
        try (Connection con = Server.getConnection(); PreparedStatement ps = con.prepareStatement("UPDATE guilds SET allianceId = ? WHERE guildid = ?")) {
            ps.setInt(1, aid);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int getIncreaseGuildCost(int size) {
        return 500000 * (size - 6) / 6;
    }
}
