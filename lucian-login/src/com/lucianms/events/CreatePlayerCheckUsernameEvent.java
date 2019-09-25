package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleClient;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.Server;
import tools.MaplePacketCreator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author izarooni
 */
public class CreatePlayerCheckUsernameEvent extends PacketEvent {

    private String username;

    @Override
    public void processInput(MaplePacketReader reader) {
        username = reader.readMapleAsciiString();
    }

    @Override
    public Object onPacket() {
        MapleClient client = getClient();
        try (Connection con = Server.getConnection();
             PreparedStatement ps = con.prepareStatement("select * from ign_reserves where reserve = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    if (username.equalsIgnoreCase(rs.getString("reserve"))
                            && !rs.getString("username").equalsIgnoreCase(client.getAccountName())) {
                        client.announce(MaplePacketCreator.charNameResponse(username, true));
                        client.announce(MaplePacketCreator.serverNotice(0, "This ign is reserved for another user"));
                        return null;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        boolean canCreate = MapleCharacter.canCreateChar(username);
        client.announce(MaplePacketCreator.charNameResponse(username, !canCreate));
        if (canCreate) {
            client.setCreationName(username);
        }
        return null;
    }
}
