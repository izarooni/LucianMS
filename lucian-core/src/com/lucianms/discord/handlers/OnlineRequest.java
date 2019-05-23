package com.lucianms.discord.handlers;

import com.lucianms.client.MapleCharacter;
import com.lucianms.discord.DiscordConnection;
import com.lucianms.discord.Headers;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.nio.send.MaplePacketWriter;
import com.lucianms.server.Server;
import com.lucianms.server.channel.MapleChannel;
import com.lucianms.server.world.MapleWorld;

import java.util.Collection;
import java.util.List;

/**
 * @author izarooni
 */
public class OnlineRequest extends DiscordRequest {

    @Override
    public void handle(MaplePacketReader reader) {
        List<MapleWorld> worlds = Server.getWorlds();

        MaplePacketWriter w = new MaplePacketWriter();
        w.write(Headers.Online.value);
        w.writeLong(reader.readLong()); // Channel_ID
        w.write(worlds.size());
        for (MapleWorld world : worlds) {
            List<MapleChannel> channels = world.getChannels();
            w.write(channels.size());
            for (MapleChannel channel : channels) {
                Collection<MapleCharacter> players = world.getPlayers(p -> (!p.isGM() || !p.isHidden()) && p.getClient().getChannel() == channel.getId());
                w.writeShort(players.size());
                for (MapleCharacter player : players) {
                    w.writeMapleString(player.getName());
                }
                players.clear();
            }
        }

        DiscordConnection.sendPacket(w.getPacket());
    }
}
