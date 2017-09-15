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
import net.AbstractMaplePacketHandler;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class CancelChairHandler extends AbstractMaplePacketHandler {

    @Override
    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient client) {
        final MapleCharacter player = client.getPlayer();

        int id = slea.readShort();
        if (id == -1) { // Cancel Chair
            player.setChair(0);
            client.announce(MaplePacketCreator.cancelChair(-1));
            player.getMap().broadcastMessage(player, MaplePacketCreator.showChair(player.getId(), 0), false);
        } else { // Use In-Map Chair
            player.setChair(id);
            client.announce(MaplePacketCreator.cancelChair(id));
        }
        if (player.getFishingTask() != null && !player.getFishingTask().isCanceled()) {
            player.getFishingTask().cancel();
            player.dropMessage(5, "You stopped fishing");
            player.announce(MaplePacketCreator.earnTitleMessage("You stopped fishing"));
        }
    }
}

