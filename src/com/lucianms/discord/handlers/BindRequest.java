package com.lucianms.discord.handlers;

import client.MapleCharacter;
import com.lucianms.discord.DiscordSession;
import com.lucianms.discord.Headers;
import net.server.Server;
import net.server.channel.Channel;
import net.server.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.Database;
import tools.Randomizer;
import tools.data.input.GenericLittleEndianAccessor;
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
    public void handle(GenericLittleEndianAccessor lea) {
        long channelId = lea.readLong();
        long authorId = lea.readLong();
        String accountUsername = lea.readMapleAsciiString();

        MaplePacketLittleEndianWriter writer = new MaplePacketLittleEndianWriter();
        writer.write(Headers.Bind.value);
        writer.writeLong(channelId);
        writer.writeLong(authorId);

        try {
            int accountId = 0;
            try (Connection con = Database.getConnection(); PreparedStatement ps = con.prepareStatement("select discord_id, id, loggedin from accounts where name = ?")) {
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
                try (Connection con = Database.getConnection(); PreparedStatement ps = con.prepareStatement("select id from characters where accountid = ?")) {
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

                            for (World world : Server.getInstance().getWorlds()) {
                                for (Channel channel : world.getChannels()) {
                                    MapleCharacter player = channel.getPlayerStorage().getCharacterById(rs.getInt("id"));
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
