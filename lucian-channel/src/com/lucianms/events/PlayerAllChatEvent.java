package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.command.CommandWorker;
import com.lucianms.discord.DiscordConnection;
import com.lucianms.discord.Headers;
import com.lucianms.discord.handlers.BindRequest;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.nio.send.MaplePacketWriter;
import tools.MaplePacketCreator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author izarooni
 */
public class PlayerAllChatEvent extends PacketEvent {

    private String content;
    private byte shout;

    @Override
    public void processInput(MaplePacketReader reader) {
        content = reader.readMapleAsciiString();
        shout = reader.readByte();
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        if (getClient().getDiscordKey() != null && getClient().getDiscordId() > 0 && content.equals(getClient().getDiscordKey())) {
            MaplePacketWriter w = new MaplePacketWriter();
            w.write(Headers.Bind.value);
            w.writeLong(0); // ignore channel_id
            w.writeLong(getClient().getDiscordId());
            w.write(3);
            DiscordConnection.sendPacket(w.getPacket());
            BindRequest.keys.remove(getClient().getDiscordKey());
            getClient().setDiscordKey(null);
            try (Connection con = getClient().getWorldServer().getConnection();
                 PreparedStatement ps = con.prepareStatement("update accounts set discord_id = ? where id = ?")) {
                ps.setLong(1, getClient().getDiscordId());
                ps.setInt(2, player.getAccountID());
                ps.executeUpdate();
                player.dropMessage("Success! Your Discord account is now bound to your game account");
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        }
        if (!player.isMuted() || player.isGM()) {
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
                player.getChatType().sendChat(player, content, shout != 0);
            } else {
                player.getMap().broadcastGMMessage(MaplePacketCreator.serverNotice(2, player.getClient().getChannel(), String.format("[hide] %s : %s", player.getName(), content)));
                player.getMap().broadcastGMMessage(MaplePacketCreator.getChatText(player.getId(), content, false, true));
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

