package com.lucianms.discord.handlers;

import com.lucianms.client.MapleCharacter;
import com.lucianms.discord.DiscordConnection;
import com.lucianms.discord.Headers;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.nio.send.MaplePacketWriter;
import com.lucianms.server.Server;
import com.lucianms.server.world.MapleWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.Randomizer;

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

        MaplePacketWriter w = new MaplePacketWriter();
        w.write(Headers.Bind.value);
        w.writeLong(channelId);
        w.writeLong(authorId);

        try (Connection con = DiscordConnection.getDatabaseConnection()) {
            int accountId = 0;
            try (PreparedStatement ps = con.prepareStatement("select discord_id, id, loggedin from accounts where name = ?")) {
                ps.setString(1, accountUsername);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        if (rs.getString("discord_id") != null) {
                            w.write(2);
                            w.writeMapleString(accountUsername);
                            DiscordConnection.sendPacket(w.getPacket());
                            return;
                        }
                        if (rs.getInt("loggedin") == 2) {
                            accountId = rs.getInt("id");
                        }
                    }
                }
            }
            if (accountId == 0) {
                w.write(0);
                w.writeMapleString(accountUsername);
                DiscordConnection.sendPacket(w.getPacket());
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
                            w.write(1);
                            w.writeMapleString(key);
                            w.writeMapleString(accountUsername);
                            DiscordConnection.sendPacket(w.getPacket());

                            for (MapleWorld world : Server.getWorlds()) {
                                MapleCharacter player = world.getPlayerStorage().get(rs.getInt("id"));
                                if (player != null) {
                                    player.getClient().setDiscordId(authorId);
                                    player.getClient().setDiscordKey(key);
                                    return;
                                }
                            }
                        }
                        w.write(0);
                        w.writeMapleString(accountUsername);
                        DiscordConnection.sendPacket(w.getPacket());
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
