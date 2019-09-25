package com.lucianms.discord.handlers;

import com.lucianms.client.MapleCharacter;
import com.lucianms.discord.DiscordConnection;
import com.lucianms.discord.Headers;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.nio.send.MaplePacketWriter;
import com.lucianms.server.Server;
import com.lucianms.server.world.MapleWorld;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author izarooni
 */
public class DisconnectRequest extends DiscordRequest {

    private enum Result {
        Success, Failure, NotFound
    }

    @Override
    public void handle(MaplePacketReader reader) {
        byte action = reader.readByte();

        MaplePacketWriter writer = new MaplePacketWriter();
        writer.write(Headers.Disconnect.value);
        writer.write(action);

        if (action == 0) {
            DisconnectChannel(reader, writer);
        } else if (action == 1) {
            DisconnectDM(reader, writer);
        }
    }

    /**
     * Invoked via command usage in a private message
     */
    private void DisconnectDM(MaplePacketReader reader, MaplePacketWriter writer) {
        long userID = reader.readLong();
        writer.writeLong(userID);

        try (Connection con = DiscordConnection.getDatabaseConnection()) {
            try (PreparedStatement ps = con.prepareStatement("select id, name from characters where accountid = (select id from accounts where discord_id = ?)")) {
                ps.setLong(1, userID);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        for (MapleWorld world : Server.getWorlds()) {
                            MapleCharacter target = world.getPlayerStorage().get(rs.getInt("id"));
                            if (target != null) {
                                world.getPlayerStorage().remove(target.getId());
                                target.getClient().disconnect();
                                writer.write(Result.Success.ordinal());
                                writer.writeMapleString(target.getName());
                                DiscordConnection.sendPacket(writer.getPacket());
                                return;
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            writer.write(Result.Failure.ordinal());
            DiscordConnection.sendPacket(writer.getPacket());
            return;
        }
        writer.write(Result.NotFound.ordinal());
        DiscordConnection.sendPacket(writer.getPacket());
    }

    /**
     * Invoked via command usage in the Discord server
     */
    private void DisconnectChannel(MaplePacketReader reader, MaplePacketWriter writer) {
        final long channelID = reader.readLong();
        String username = reader.readMapleAsciiString();

        boolean online = false;

        writer.writeLong(channelID);

        for (MapleWorld world : Server.getWorlds()) {
            MapleCharacter player = world.findPlayer(p -> p.getName().equalsIgnoreCase(username));
            if (player != null) {
                online = true;
                world.getPlayerStorage().remove(player.getId());
                player.getClient().disconnect();
            }
        }

        if (online) {
            writer.writeBoolean(true);
        } else {
            try {
                int accountID = 0;
                try (Connection con = DiscordConnection.getDatabaseConnection();
                     PreparedStatement ps = con.prepareStatement("SELECT accountid FROM characters WHERE name = ?")) {
                    ps.setString(1, username);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            accountID = rs.getInt("accountid");
                        }
                    }
                }
                if (accountID > 0) {
                    try (Connection con = DiscordConnection.getDatabaseConnection();
                         PreparedStatement ps = con.prepareStatement("UPDATE accounts SET loggedin = 0 WHERE id = ?")) {
                        ps.setInt(1, accountID);
                        ps.executeUpdate();
                        writer.write(Result.Success.ordinal());
                    }
                } else {
                    writer.write(Result.NotFound.ordinal());
                }
            } catch (SQLException e) {
                e.printStackTrace();
                writer.write(Result.Failure.ordinal());
            }
        }
        DiscordConnection.sendPacket(writer.getPacket());
    }
}
