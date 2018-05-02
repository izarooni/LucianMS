package com.lucianms.discord.handlers;

import client.MapleCharacter;
import com.lucianms.discord.DiscordSession;
import com.lucianms.discord.Headers;
import net.server.Server;
import net.server.channel.Channel;
import net.server.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.data.input.GenericLittleEndianAccessor;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.util.List;

/**
 * @author izarooni
 */
public class OnlineRequest extends DiscordRequest {

    private static final Logger LOGGER = LoggerFactory.getLogger(OnlineRequest.class);

    @Override
    public void handle(GenericLittleEndianAccessor lea) {
        Server server = Server.getInstance();
        List<World> worlds = server.getWorlds();

        MaplePacketLittleEndianWriter writer = new MaplePacketLittleEndianWriter();
        writer.write(Headers.Online.value);
        writer.writeLong(lea.readLong()); // Channel_ID
        writer.write(worlds.size());
        for (World world : worlds) {
            List<Channel> channels = world.getChannels();
            writer.write(channels.size());
            for (Channel channel : channels) {
                writer.writeShort(channel.getPlayerStorage().size());
                for (MapleCharacter players : channel.getPlayerStorage().getAllCharacters()) {
                    writer.writeMapleAsciiString(players.getName());
                }
            }
        }

        DiscordSession.sendPacket(writer.getPacket());
    }
}
