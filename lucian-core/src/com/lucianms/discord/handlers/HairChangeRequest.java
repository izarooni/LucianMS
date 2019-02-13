package com.lucianms.discord.handlers;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleStat;
import com.lucianms.discord.DiscordConnection;
import com.lucianms.discord.Headers;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author izarooni
 */
public class HairChangeRequest extends DiscordRequest {

    private static final Logger LOGGER = LoggerFactory.getLogger(HairChangeRequest.class);

    @Override
    public void handle(MaplePacketReader reader) {
        long channelId = reader.readLong();
        String username = reader.readMapleAsciiString();
        int hairId = reader.readInt();
        LOGGER.info("Updating {}'s hair to {}", username, hairId);

        MaplePacketLittleEndianWriter writer = new MaplePacketLittleEndianWriter();
        writer.write(Headers.SetHair.value);
        writer.writeLong(channelId);
        writer.writeMapleAsciiString(username);

        MapleCharacter player = Server.getWorld(0).findPlayer(p -> p.getName().equalsIgnoreCase(username));
        if (player != null) {
            player.setHair(hairId);
            player.updateSingleStat(MapleStat.HAIR, hairId);
            player.equipChanged();
            writer.write(1);
        } else {
            int playerId = MapleCharacter.getIdByName(username);
            if (playerId > 0) {
                try (Connection con = DiscordConnection.getDatabaseConnection();
                     PreparedStatement ps = con.prepareStatement("update characters set hair = ? where id = ?")) {
                    ps.setInt(1, hairId);
                    ps.setInt(2, playerId);
                    ps.executeUpdate();
                    writer.write(2);
                } catch (SQLException e) {
                    writer.write(-1);
                    LOGGER.info("Unable to update {}'s hair", username, e);
                }
            } else {
                writer.write(0);
            }
        }
        DiscordConnection.sendPacket(writer.getPacket());
    }
}
