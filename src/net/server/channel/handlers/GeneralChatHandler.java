/*
This file is part of the OdinMS Maple Story Server
Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
Matthias Butz <matze@odinms.de>
Jan Christian Meyer <vimes@odinms.de>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation version 3 as published by
the Free Software Foundation. You may not use, modify or distribute
this program under any other version of the GNU Affero General Public
License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleClient;
import com.lucianms.command.CommandWorker;
import com.lucianms.discord.DiscordSession;
import com.lucianms.discord.Headers;
import com.lucianms.discord.handlers.BindRequest;
import tools.DatabaseConnection;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class GeneralChatHandler extends net.AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient client) {
        MapleCharacter player = client.getPlayer();
        String message = slea.readMapleAsciiString();
        if (client.getDiscordKey() != null && client.getDiscordId() > 0 && message.equals(client.getDiscordKey())) {
            MaplePacketLittleEndianWriter writer = new MaplePacketLittleEndianWriter();
            writer.write(Headers.Bind.value);
            writer.writeLong(0); // ignore channel_id
            writer.writeLong(client.getDiscordId());
            writer.write(3);
            DiscordSession.sendPacket(writer.getPacket());
            BindRequest.keys.remove(client.getDiscordKey());
            client.setDiscordKey(null);
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("update accounts set discord_id = ? where id = ?")) {
                ps.setLong(1, client.getDiscordId());
                ps.setInt(2, player.getAccountID());
                ps.executeUpdate();
                player.dropMessage("Success! Your Discord account is not bound to this LucianMS in-game account");
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return;
        }
        if (!player.isMuted()) {
            if (CommandWorker.isCommand(message)) {
                if (CommandWorker.process(client, message, false)) {
                    return;
                }
            }
            int show = slea.readByte();
            if (player.getMap().isMuted() && !player.isGM()) {
                player.dropMessage(5, "The map you are in is currently muted. Please try again later.");
                return;
            }
            if (!player.isHidden()) {
                player.getChatType().sendChat(player, message, show);
                //                player.getMap().broadcastMessage(MaplePacketCreator.getChatText(player.getId(), message, player.getWhiteChat(), show));
            } else {
                player.getMap().broadcastGMMessage(MaplePacketCreator.serverNotice(2, player.getClient().getChannel(), String.format("[hide] %s : %s", player.getName(), message)));
                player.getMap().broadcastGMMessage(MaplePacketCreator.getChatText(player.getId(), message, false, 1));
            }
        } else {
            player.dropMessage(5, "You have been muted, meaning you cannot speak.");
        }
    }
}

