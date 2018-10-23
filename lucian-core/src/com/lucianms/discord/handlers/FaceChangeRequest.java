package com.lucianms.discord.handlers;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleStat;
import com.lucianms.discord.DiscordSession;
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
public class FaceChangeRequest extends DiscordRequest {

    private static final Logger LOGGER = LoggerFactory.getLogger(FaceChangeRequest.class);

    @Override
    public void handle(MaplePacketReader reader) {
        long channelId = reader.readLong();
        String username = reader.readMapleAsciiString();
        int faceId = reader.readInt();
        LOGGER.info("Updating {}'s face to {}", username, faceId);

        MaplePacketLittleEndianWriter writer = new MaplePacketLittleEndianWriter();
        writer.write(Headers.SetFace.value);
        writer.writeLong(channelId);
        writer.writeMapleAsciiString(username);


        MapleCharacter player = Server.getWorld(0).getPlayerStorage().getPlayerByName(username);
        if (player != null) {
            player.setFace(faceId);
            player.updateSingleStat(MapleStat.FACE, faceId);
            player.equipChanged();

            writer.write(1);
        } else {
            int playerId = MapleCharacter.getIdByName(username);
            if (playerId > 0) {
                try (Connection con = DiscordSession.getConnection();
                     PreparedStatement ps = con.prepareStatement("update characters set face = ? where id = ?")) {
                    ps.setInt(1, faceId);
                    ps.setInt(2, playerId);
                    ps.executeUpdate();
                    writer.write(2);
                } catch (SQLException e) {
                    writer.write(-1);
                    LOGGER.info("Unable to update {}'s face", username, e);
                }
            } else {
                writer.write(0);
                LOGGER.info("The player {} could not be found", username);
            }
        }
        DiscordSession.getSession().write(writer.getPacket());
    }
}