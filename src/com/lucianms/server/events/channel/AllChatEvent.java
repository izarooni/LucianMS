package com.lucianms.server.events.channel;

import client.MapleCharacter;
import com.lucianms.command.CommandWorker;
import com.lucianms.discord.DiscordSession;
import com.lucianms.discord.Headers;
import com.lucianms.discord.handlers.BindRequest;
import net.PacketEvent;
import tools.DatabaseConnection;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author izarooni
 */
public class AllChatEvent extends PacketEvent {

    private String content;
    private byte shout;

    @Override
    public void process(SeekableLittleEndianAccessor slea) {
        content = slea.readMapleAsciiString();
        shout = slea.readByte();
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        if (getClient().getDiscordKey() != null && getClient().getDiscordId() > 0 && content.equals(getClient().getDiscordKey())) {
            MaplePacketLittleEndianWriter writer = new MaplePacketLittleEndianWriter();
            writer.write(Headers.Bind.value);
            writer.writeLong(0); // ignore channel_id
            writer.writeLong(getClient().getDiscordId());
            writer.write(3);
            DiscordSession.sendPacket(writer.getPacket());
            BindRequest.keys.remove(getClient().getDiscordKey());
            getClient().setDiscordKey(null);
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("update accounts set discord_id = ? where id = ?")) {
                ps.setLong(1, getClient().getDiscordId());
                ps.setInt(2, player.getAccountID());
                ps.executeUpdate();
                player.dropMessage("Success! Your Discord account is not bound to this LucianMS in-game account");
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        }
        if (!player.isMuted()) {
            if (CommandWorker.isCommand(content)) {
                if (CommandWorker.process(getClient(), content, false)) {
                    return null;
                }
            }
            if (player.getMap().isMuted() && !player.isGM()) {
                player.dropMessage(5, "The map you are in is currently muted. Please try again later.");
                return null;
            }
            if (!player.isHidden()) {
                player.getChatType().sendChat(player, content, shout);
                //                player.getMap().broadcastMessage(MaplePacketCreator.getChatText(player.getId(), content, player.getWhiteChat(), show));
            } else {
                player.getMap().broadcastGMMessage(MaplePacketCreator.serverNotice(2, player.getClient().getChannel(), String.format("[hide] %s : %s", player.getName(), content)));
                player.getMap().broadcastGMMessage(MaplePacketCreator.getChatText(player.getId(), content, false, 1));
            }
        } else {
            player.dropMessage(5, "You have been muted, meaning you cannot speak.");
        }
        return null;
    }

    public String getContent() {
        return content;
    }
}
