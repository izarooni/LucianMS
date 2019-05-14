package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleClient;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.Server;
import com.lucianms.server.world.MapleWorld;
import tools.MaplePacketCreator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Date;

/**
 * @author BubblesDev
 * @author izarooni
 */
public class PlayerReportedEvent extends PacketEvent {

    private String username;
    private String content;
    private String log;
    private byte action;
    private byte reason;

    @Override
    public void processInput(MaplePacketReader reader) {
        action = reader.readByte();
        username = reader.readMapleAsciiString();
        reason = reader.readByte();
        content = reader.readMapleAsciiString();
        if (action == 1) {
            log = reader.readMapleAsciiString();
        }
    }

    @Override
    public Object onPacket() {
        MapleClient client = getClient();
        MapleWorld world = client.getWorldServer();
        MapleCharacter player = client.getPlayer();

        if (action == 0) {
            if (player.getPossibleReports() > 0) {
                if (player.getMeso() > 299) {
                    player.decreaseReports();
                    player.gainMeso(-300, true);
                } else {
                    client.announce(MaplePacketCreator.reportResponse((byte) 4));
                    return null;
                }
            } else {
                client.announce(MaplePacketCreator.reportResponse((byte) 2));
                return null;
            }
            world.sendMessage(p -> p.getGMLevel() > 0, 6, "[Report] '{}' reported '{}' for: {}", player.getName(), username, content);
            addReport(player.getId(), MapleCharacter.getIdByName(username), 0, content, null);
        } else if (action == 1) {
            if (log == null) {
                return null;
            }
            if (player.getPossibleReports() > 0) {
                if (player.getMeso() > 299) {
                    player.decreaseReports();
                    player.gainMeso(-300, true);
                } else {
                    client.announce(MaplePacketCreator.reportResponse((byte) 4));
                    return null;
                }
            }
            world.sendMessage(p -> p.getGMLevel() > 0, 6, "[Report] '{}' reported '{}' for: {}", player.getName(), username, content);
            addReport(player.getId(), MapleCharacter.getIdByName(username), reason, content, log);
        }
        return null;
    }

    private static void addReport(int reporterid, int victimid, int reason, String description, String chatlog) {
        try (Connection con = Server.getConnection();
             PreparedStatement ps = con.prepareStatement("INSERT INTO reports (`reporttime`, `reporterid`, `victimid`, `reason`, `chatlog`, `description`) VALUES (?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, DateFormat.getInstance().format(new Date()));
            ps.setInt(2, reporterid);
            ps.setInt(3, victimid);
            ps.setInt(4, reason);
            ps.setString(5, chatlog);
            ps.setString(6, description);
            ps.addBatch();
            ps.executeBatch();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}
