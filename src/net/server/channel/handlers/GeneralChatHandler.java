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
import command.CommandWorker;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public class GeneralChatHandler extends net.AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient client) {
        MapleCharacter player = client.getPlayer();
        if (!player.isMuted()) {
            String message = slea.readMapleAsciiString();
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

