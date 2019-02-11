package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleClient;
import com.lucianms.nio.receive.MaplePacketReader;
import tools.MaplePacketCreator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author izarooni
 */
public class PlayerGuildBoardInteractionEvent extends PacketEvent {

    private static String correctLength(String in, int maxSize) {
        return in.length() > maxSize ? in.substring(0, maxSize) : in;
    }

    private String title;
    private String content;
    private byte action;
    private int start;
    private int localThreadID;
    private int threadID;
    private int iconID;
    private boolean edit;
    private boolean notice;

    @Override
    public void processInput(MaplePacketReader reader) {
        action = reader.readByte();
        switch (action) {
            case 0:
                edit = reader.readByte() == 1;
                if (edit) {
                    localThreadID = reader.readInt();
                }
                notice = reader.readByte() == 1;
                title = reader.readMapleAsciiString();
                content = reader.readMapleAsciiString();
                iconID = reader.readInt();
                break;
            case 1:
            case 3:
                localThreadID = reader.readInt();
                break;
            case 2:
                start = reader.readInt();
                break;
            case 4:
                localThreadID = reader.readInt();
                content = reader.readMapleAsciiString();
                break;
            case 5:
                reader.readInt();
                threadID = reader.readInt();
                break;
        }
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        if (player.getGuildId() < 1) {
            return null;
        }
        switch (action) {
            case 0: {
                String title = correctLength(this.title, 25);
                String content = correctLength(this.content, 600);
                if (iconID >= 0x64 && iconID <= 0x6a) {
                    if (player.getItemQuantity(5290000 + iconID - 0x64, false) > 0) {
                        return null;
                    }
                } else if (iconID < 0 || iconID > 3) {
                    return null;
                }
                if (!edit) {
                    newBBSThread(getClient(), title, content, iconID, notice);
                } else {
                    editBBSThread(getClient(), title, content, iconID, localThreadID);
                }
                break;
            }
            case 1:
                deleteBBSThread(getClient(), localThreadID);
                break;
            case 2:
                listBBSThreads(getClient(), start * 10);
                break;
            case 3: // list thread + reply, followed by id (int)
                displayThread(getClient(), localThreadID);
                break;
            case 4: // reply
                content = correctLength(content, 25);
                newBBSReply(getClient(), localThreadID, content);
                break;
            case 5: // delete reply
                deleteBBSReply(getClient(), threadID);
                break;
        }
        return null;
    }

    private static void listBBSThreads(MapleClient c, int start) {
        try (Connection con = c.getWorldServer().getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM bbs_threads WHERE guildid = ? ORDER BY localthreadid DESC")) {
            ps.setInt(1, c.getPlayer().getGuildId());
            try (ResultSet rs = ps.executeQuery()) {
                c.announce(MaplePacketCreator.BBSThreadList(rs, start));
            }
        } catch (SQLException se) {
            se.printStackTrace();
        }
    }

    private static void newBBSReply(MapleClient c, int localthreadid, String text) {
        if (c.getPlayer().getGuildId() <= 0) {
            return;
        }
        try (Connection con = c.getWorldServer().getConnection()) {
            int threadid;
            try (PreparedStatement ps = con.prepareStatement("SELECT threadid FROM bbs_threads WHERE guildid = ? AND localthreadid = ?")) {
                ps.setInt(1, c.getPlayer().getGuildId());
                ps.setInt(2, localthreadid);
                try (ResultSet threadRS = ps.executeQuery()) {
                    if (!threadRS.next()) {
                        return;
                    }
                    threadid = threadRS.getInt("threadid");
                }
            }
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO bbs_replies " + "(`threadid`, `postercid`, `timestamp`, `content`) VALUES " + "(?, ?, ?, ?)")) {
                ps.setInt(1, threadid);
                ps.setInt(2, c.getPlayer().getId());
                ps.setLong(3, System.currentTimeMillis());
                ps.setString(4, text);
                ps.execute();
            }
            try (PreparedStatement ps = con.prepareStatement("UPDATE bbs_threads SET replycount = replycount + 1 WHERE threadid = ?")) {
                ps.setInt(1, threadid);
                ps.execute();
            }
            displayThread(c, localthreadid);
        } catch (SQLException se) {
            se.printStackTrace();
        }
    }

    private static void editBBSThread(MapleClient client, String title, String text, int icon, int localthreadid) {
        MapleCharacter c = client.getPlayer();
        if (c.getGuildId() < 1) {
            return;
        }
        try (Connection con = client.getWorldServer().getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE bbs_threads SET `name` = ?, `timestamp` = ?, " + "`icon` = ?, " + "`startpost` = ? WHERE guildid = ? AND localthreadid = ? AND (postercid = ? OR ?)")) {
            ps.setString(1, title);
            ps.setLong(2, System.currentTimeMillis());
            ps.setInt(3, icon);
            ps.setString(4, text);
            ps.setInt(5, c.getGuildId());
            ps.setInt(6, localthreadid);
            ps.setInt(7, c.getId());
            ps.setBoolean(8, c.getGuildRank() < 3);
            ps.execute();
            displayThread(client, localthreadid);
        } catch (SQLException se) {
            se.printStackTrace();
        }
    }

    private static void newBBSThread(MapleClient client, String title, String text, int icon, boolean bNotice) {
        MapleCharacter c = client.getPlayer();
        if (c.getGuildId() <= 0) {
            return;
        }
        int nextId = 0;
        try (Connection con = client.getWorldServer().getConnection()) {
            if (!bNotice) {
                try (PreparedStatement ps = con.prepareStatement("SELECT MAX(localthreadid) AS lastLocalId FROM bbs_threads WHERE guildid = ?")) {
                    ps.setInt(1, c.getGuildId());
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        nextId = rs.getInt("lastLocalId") + 1;
                    }
                }
            }
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO bbs_threads " + "(`postercid`, `name`, `timestamp`, `icon`, `startpost`, " + "`guildid`, `localthreadid`) " + "VALUES(?, ?, ?, ?, ?, ?, ?)")) {
                ps.setInt(1, c.getId());
                ps.setString(2, title);
                ps.setLong(3, System.currentTimeMillis());
                ps.setInt(4, icon);
                ps.setString(5, text);
                ps.setInt(6, c.getGuildId());
                ps.setInt(7, nextId);
                ps.execute();
            }
            displayThread(client, nextId);
        } catch (SQLException se) {
            se.printStackTrace();
        }
    }

    public static void deleteBBSThread(MapleClient client, int localthreadid) {
        MapleCharacter mc = client.getPlayer();
        if (mc.getGuildId() <= 0) {
            return;
        }
        try (Connection con = client.getWorldServer().getConnection()) {
            int threadid;
            try (PreparedStatement ps = con.prepareStatement("SELECT threadid, postercid FROM bbs_threads WHERE guildid = ? AND localthreadid = ?")) {
                ps.setInt(1, mc.getGuildId());
                ps.setInt(2, localthreadid);
                try (ResultSet threadRS = ps.executeQuery()) {
                    if (!threadRS.next()) {
                        return;
                    }
                    if (mc.getId() != threadRS.getInt("postercid") && mc.getGuildRank() > 2) {
                        return;
                    }
                    threadid = threadRS.getInt("threadid");
                }
            }
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM bbs_replies WHERE threadid = ?")) {
                ps.setInt(1, threadid);
                ps.execute();
            }
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM bbs_threads WHERE threadid = ?")) {
                ps.setInt(1, threadid);
                ps.execute();
            }
        } catch (SQLException se) {
            se.printStackTrace();
        }
    }

    public static void deleteBBSReply(MapleClient client, int replyid) {
        MapleCharacter mc = client.getPlayer();
        if (mc.getGuildId() <= 0) {
            return;
        }
        int threadid;
        try (Connection con = client.getWorldServer().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT postercid, threadid FROM bbs_replies WHERE replyid = ?")) {
                ps.setInt(1, replyid);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return;
                    }
                    if (mc.getId() != rs.getInt("postercid") && mc.getGuildRank() > 2) {
                        return;
                    }
                    threadid = rs.getInt("threadid");
                }
            }
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM bbs_replies WHERE replyid = ?")) {
                ps.setInt(1, replyid);
                ps.execute();
            }
            try (PreparedStatement ps = con.prepareStatement("UPDATE bbs_threads SET replycount = replycount - 1 WHERE threadid = ?")) {
                ps.setInt(1, threadid);
                ps.execute();
            }
            displayThread(client, threadid, false);
        } catch (SQLException se) {
            se.printStackTrace();
        }
    }

    public static void displayThread(MapleClient client, int threadid) {
        displayThread(client, threadid, true);
    }

    public static void displayThread(MapleClient client, int threadid, boolean bIsThreadIdLocal) {
        MapleCharacter mc = client.getPlayer();
        if (mc.getGuildId() <= 0) {
            return;
        }
        try (Connection con = client.getWorldServer().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM bbs_threads WHERE guildid = ? AND " + (bIsThreadIdLocal ? "local" : "") + "threadid = ?")) {
                ps.setInt(1, mc.getGuildId());
                ps.setInt(2, threadid);
                try (ResultSet threadRS = ps.executeQuery()) {
                    if (threadRS.next()) {
                        if (threadRS.getInt("replycount") >= 0) {
                            try (PreparedStatement ps2 = con.prepareStatement("SELECT * FROM bbs_replies WHERE threadid = ?")) {
                                ps2.setInt(1, !bIsThreadIdLocal ? threadid : threadRS.getInt("threadid"));
                                try (ResultSet repliesRS = ps2.executeQuery()) {
                                    client.announce(MaplePacketCreator.showThread(bIsThreadIdLocal ? threadid : threadRS.getInt("localthreadid"), threadRS, repliesRS));
                                }
                            }
                        }
                    }
                }
            }
        } catch (SQLException se) {
            se.printStackTrace();
        } catch (RuntimeException re) {//btw we get this everytime for some reason, but replies work!
            re.printStackTrace();
            System.out.println("The number of reply rows does not match the replycount in thread.");
        }
    }
}
