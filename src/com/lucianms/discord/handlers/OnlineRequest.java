package com.lucianms.discord.handlers;

import client.MapleCharacter;
import com.lucianms.discord.DiscordSession;
import com.lucianms.discord.Headers;
import com.lucianms.nio.receive.MaplePacketReader;
import net.server.Server;
import net.server.channel.MapleChannel;
import net.server.world.MapleWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.util.List;

/**
 * @author izarooni
 */
public class OnlineRequest extends DiscordRequest {

    private static final Logger LOGGER = LoggerFactory.getLogger(OnlineRequest.class);

    @Override
    public void handle(MaplePacketReader reader) {
        Server server = Server.getInstance();
        List<MapleWorld> worlds = server.getWorlds();

        MaplePacketLittleEndianWriter writer = new MaplePacketLittleEndianWriter();
        writer.write(Headers.Online.value);
        writer.writeLong(reader.readLong()); // Channel_ID
        writer.write(worlds.size());
        for (MapleWorld world : worlds) {
            List<MapleChannel> channels = world.getChannels();
            writer.write(channels.size());
            for (MapleChannel channel : channels) {
                writer.writeShort(channel.getPlayerStorage().size());
                for (MapleCharacter players : channel.getPlayerStorage().getAllPlayers()) {
                    writer.writeMapleAsciiString(players.getName());
                }
            }
        }

        DiscordSession.sendPacket(writer.getPacket());
    }
}
