package com.lucianms.discord.handlers;

import client.MapleCharacter;
import com.lucianms.discord.DiscordSession;
import com.lucianms.discord.Headers;
import net.server.Server;
import net.server.world.World;
import tools.DatabaseConnection;
import tools.data.input.GenericLittleEndianAccessor;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author izarooni
 */
public class DisconnectRequest extends DiscordRequest {

    @Override
    public void handle(GenericLittleEndianAccessor lea) {
        final long channelID = lea.readLong();
        String username = lea.readMapleAsciiString();

        boolean online = false;

        MaplePacketLittleEndianWriter writer = new MaplePacketLittleEndianWriter();
        writer.write(Headers.Disconnect.value);
        writer.writeLong(channelID);

        for (World world : Server.getInstance().getWorlds()) {
            MapleCharacter player = world.getPlayerStorage().getCharacterByName(username);
            if (player != null) {
                online = true;
                player.getClient().disconnect(false, false);
            }
        }

        if (online) {
            writer.writeBool(true);
        } else {
            try {
                int accountID = 0;
                try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT accountid FROM characters WHERE name = ?")) {
                    ps.setString(1, username);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            accountID = rs.getInt("accountid");
                        }
                    }
                }
                if (accountID > 0) {
                    try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE accounts SET loggedin = 0 WHERE id = ?")) {
                        ps.setInt(1, accountID);
                        ps.executeUpdate();
                        writer.writeBool(true);
                    }
                } else {
                    writer.writeBool(false);
                }
            } catch (SQLException e) {
                e.printStackTrace();
                writer.writeBool(false);
            }
        }
        DiscordSession.sendPacket(writer.getPacket());
    }
}
