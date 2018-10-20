package com.lucianms.discord.handlers;

import com.lucianms.client.MapleCharacter;
import com.lucianms.discord.DiscordSession;
import com.lucianms.discord.Headers;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.Server;
import com.lucianms.server.channel.MapleChannel;
import com.lucianms.server.world.MapleWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.Randomizer;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * @author izarooni
 */
public class BindRequest extends DiscordRequest {

    private static final Logger LOGGER = LoggerFactory.getLogger(BindRequest.class);
    public static final ArrayList<String> keys = new ArrayList<>();

    @Override
    public void handle(MaplePacketReader reader) {
        long channelId = reader.readLong();
        long authorId = reader.readLong();
        String accountUsername = reader.readMapleAsciiString();

        MaplePacketLittleEndianWriter writer = new MaplePacketLittleEndianWriter();
        writer.write(Headers.Bind.value);
        writer.writeLong(channelId);
        writer.writeLong(authorId);

        try (Connection con = DiscordSession.getConnection()) {
            int accountId = 0;
            try (PreparedStatement ps = con.prepareStatement("select discord_id, id, loggedin from accounts where name = ?")) {
                ps.setString(1, accountUsername);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        if (rs.getString("discord_id") != null) {
                            writer.write(2);
                            writer.writeMapleAsciiString(accountUsername);
                            DiscordSession.sendPacket(writer.getPacket());
                            return;
                        }
                        if (rs.getInt("loggedin") == 2) {
                            accountId = rs.getInt("id");
                        }
                    }
                }
            }
            if (accountId == 0) {
                writer.write(0);
                writer.writeMapleAsciiString(accountUsername);
                DiscordSession.sendPacket(writer.getPacket());
            } else {
                try (PreparedStatement ps = con.prepareStatement("select id from characters where accountid = ?")) {
                    ps.setInt(1, accountId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String key;
                            do {
                                key = Randomizer.nextString(8);
                            } while (keys.contains(key));
                            keys.add(key);
                            writer.write(1);
                            writer.writeMapleAsciiString(key);
                            writer.writeMapleAsciiString(accountUsername);
                            DiscordSession.sendPacket(writer.getPacket());

                            for (MapleWorld world : Server.getWorlds()) {
                                for (MapleChannel channel : world.getChannels()) {
                                    MapleCharacter player = channel.getPlayerStorage().getPlayerByID(rs.getInt("id"));
                                    if (player != null) {
                                        player.getClient().setDiscordId(authorId);
                                        player.getClient().setDiscordKey(key);
                                        return;
                                    }
                                }
                            }
                        }
                        writer.write(0);
                        writer.writeMapleAsciiString(accountUsername);
                        DiscordSession.sendPacket(writer.getPacket());
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
