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
import net.AbstractMaplePacketHandler;
import net.server.world.World;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 * @author Matze
 */
public final class WhisperHandler extends AbstractMaplePacketHandler {

    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient client) {
        MapleCharacter player = client.getPlayer();

        byte mode = slea.readByte();
        if (mode == 6) { // whisper
            String recipient = slea.readMapleAsciiString();
            String message = slea.readMapleAsciiString();
            if (CommandWorker.isCommand(message)) {
                if (CommandWorker.process(client, message, false)) {
                    return;
                }
            }
            MapleCharacter target = client.getChannelServer().getPlayerStorage().getCharacterByName(recipient);
            if (target != null) {
                target.getClient().announce(MaplePacketCreator.getWhisper(client.getPlayer().getName(), client.getChannel(), message));

                if (target.isHidden() && target.gmLevel() > client.getPlayer().gmLevel()) {
                    client.announce(MaplePacketCreator.getWhisperReply(recipient, (byte) 0));
                } else {
                    client.announce(MaplePacketCreator.getWhisperReply(recipient, (byte) 1));
                }
            } else {// not found
                World world = client.getWorldServer();
                if (world.isConnected(recipient)) {
                    world.whisper(client.getPlayer().getName(), recipient, client.getChannel(), message);

                    target = world.getPlayerStorage().getCharacterByName(recipient);
                    if (target != null && target.isHidden() && target.gmLevel() > client.getPlayer().gmLevel())
                        client.announce(MaplePacketCreator.getWhisperReply(recipient, (byte) 0));
                    else
                        client.announce(MaplePacketCreator.getWhisperReply(recipient, (byte) 1));
                } else {
                    client.announce(MaplePacketCreator.getWhisperReply(recipient, (byte) 0));
                }
            }
        } else if (mode == 5 || mode == 0x44) { // - /find
            String recipient = slea.readMapleAsciiString();
            MapleCharacter target = client.getChannelServer().getPlayerStorage().getCharacterByName(recipient);
            if (mode == 0x44 && target != null) {
                if (player.getBuddylist().containsVisible(target.getId()) && target.getBuddylist().containsVisible(player.getId())) {
                    // only find if they are mutual friends
                    client.announce(MaplePacketCreator.getBuddyFindResult(target, (byte) (target.getClient().getChannel() == client.getChannel() ? 1 : 3)));
                }
            } else if (target != null && client.getPlayer().gmLevel() >= target.gmLevel()) {
                if (target.getCashShop().isOpened()) {
                    client.announce(MaplePacketCreator.getFindReply(target.getName(), -1, 2));
                } else {
                    client.announce(MaplePacketCreator.getFindReply(target.getName(), target.getMap().getId(), 1));
                }
            } else { // not found
                byte channel = (byte) (client.getWorldServer().find(recipient) - 1);
                if (channel > -1) {
                    client.announce(MaplePacketCreator.getFindReply(recipient, channel, 3));
                } else {
                    client.announce(MaplePacketCreator.getWhisperReply(recipient, (byte) 0));
                }
            }
        }
    }
}
