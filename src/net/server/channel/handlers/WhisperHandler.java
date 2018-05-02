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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.lucianms.command.CommandWorker;
import net.AbstractMaplePacketHandler;
import net.server.world.World;
import tools.DatabaseConnection;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;
import client.MapleCharacter;
import client.MapleClient;

/**
 *
 * @author Matze
 */
public final class WhisperHandler extends AbstractMaplePacketHandler {
	
    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient client) {
        byte mode = slea.readByte();
        if (mode == 6) { // whisper
            String recipient = slea.readMapleAsciiString();
            String message = slea.readMapleAsciiString();
            if (CommandWorker.isCommand(message)) {
                if (CommandWorker.process(client, message, false)) {
                    return;
                }
            }
            MapleCharacter player = client.getChannelServer().getPlayerStorage().getCharacterByName(recipient);
            if (player != null) {
                player.getClient().announce(MaplePacketCreator.getWhisper(client.getPlayer().getName(), client.getChannel(), message));
                
                if(player.isHidden() && player.gmLevel() > client.getPlayer().gmLevel()) {
                    client.announce(MaplePacketCreator.getWhisperReply(recipient, (byte) 0));
                } else {
                    client.announce(MaplePacketCreator.getWhisperReply(recipient, (byte) 1));
                }
            } else {// not found
                World world = client.getWorldServer();
                    if (world.isConnected(recipient)) {
                        world.whisper(client.getPlayer().getName(), recipient, client.getChannel(), message);
                        
                        player = world.getPlayerStorage().getCharacterByName(recipient);
                        if(player.isHidden() && player.gmLevel() > client.getPlayer().gmLevel())
                            client.announce(MaplePacketCreator.getWhisperReply(recipient, (byte) 0));
                        else
                            client.announce(MaplePacketCreator.getWhisperReply(recipient, (byte) 1));
                    } else {
                        client.announce(MaplePacketCreator.getWhisperReply(recipient, (byte) 0));
                    }
            }
        } else if (mode == 5) { // - /find
            String recipient = slea.readMapleAsciiString();
            MapleCharacter victim = client.getChannelServer().getPlayerStorage().getCharacterByName(recipient);
            if (victim != null && client.getPlayer().gmLevel() >= victim.gmLevel()) {
                if (victim.getCashShop().isOpened()) {
                    client.announce(MaplePacketCreator.getFindReply(victim.getName(), -1, 2));
                //} else if (victim.inMTS()) {
                //    c.announce(MaplePacketCreator.getFindReply(victim.getName(), -1, 0));
                } else {
                    client.announce(MaplePacketCreator.getFindReply(victim.getName(), victim.getMap().getId(), 1));
                }
            } else { // not found
                try {
                    PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT gm FROM characters WHERE name = ?");
                    ps.setString(1, recipient);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        if (rs.getInt("gm") > client.getPlayer().gmLevel()) {
                            client.announce(MaplePacketCreator.getWhisperReply(recipient, (byte) 0));
                            return;
                        }
                    }
                    rs.close();
                    ps.close();
                    byte channel = (byte) (client.getWorldServer().find(recipient) - 1);
                    if (channel > -1) {
                        client.announce(MaplePacketCreator.getFindReply(recipient, channel, 3));
                    } else {
                        client.announce(MaplePacketCreator.getWhisperReply(recipient, (byte) 0));
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        } else if (mode == 0x44) {
            //Buddy find?
        }
    }
}
