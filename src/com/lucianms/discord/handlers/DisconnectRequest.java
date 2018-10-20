package com.lucianms.discord.handlers;

import client.MapleCharacter;
import com.lucianms.discord.DiscordSession;
import com.lucianms.discord.Headers;
import com.lucianms.nio.receive.MaplePacketReader;
import net.server.Server;
import net.server.world.MapleWorld;
import tools.data.output.MaplePacketLittleEndianWriter;

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

        MaplePacketLittleEndianWriter writer = new MaplePacketLittleEndianWriter();
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
    private void DisconnectDM(MaplePacketReader reader, MaplePacketLittleEndianWriter writer) {
        long userID = reader.readLong();
        writer.writeLong(userID);

        try (Connection con = DiscordSession.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("select id, name from characters where accountid = (select id from accounts where discord_id = ?)")) {
                ps.setLong(1, userID);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        for (MapleWorld world : Server.getInstance().getWorlds()) {
                            MapleCharacter target = world.getPlayerStorage().getPlayerByID(rs.getInt("id"));
                            if (target != null) {
                                world.removePlayer(target);
                                target.getClient().disconnect(false, target.getCashShop().isOpened());
                                writer.write(Result.Success.ordinal());
                                writer.writeMapleAsciiString(target.getName());
                                DiscordSession.sendPacket(writer.getPacket());
                                return;
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            writer.write(Result.Failure.ordinal());
            DiscordSession.sendPacket(writer.getPacket());
            return;
        }
        writer.write(Result.NotFound.ordinal());
        DiscordSession.sendPacket(writer.getPacket());
    }

    /**
     * Invoked via command usage in the Discord server
     */
    private void DisconnectChannel(MaplePacketReader reader, MaplePacketLittleEndianWriter writer) {
        final long channelID = reader.readLong();
        String username = reader.readMapleAsciiString();

        boolean online = false;

        writer.writeLong(channelID);

        for (MapleWorld world : Server.getInstance().getWorlds()) {
            MapleCharacter player = world.getPlayerStorage().getPlayerByName(username);
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
                try (Connection con = DiscordSession.getConnection();
                     PreparedStatement ps = con.prepareStatement("SELECT accountid FROM characters WHERE name = ?")) {
                    ps.setString(1, username);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            accountID = rs.getInt("accountid");
                        }
                    }
                }
                if (accountID > 0) {
                    try (Connection con = DiscordSession.getConnection();
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
        DiscordSession.sendPacket(writer.getPacket());
    }
}
