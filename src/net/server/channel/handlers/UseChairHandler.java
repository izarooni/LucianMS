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
import client.inventory.MapleInventoryType;
import net.AbstractMaplePacketHandler;
import tools.ArrayUtil;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class UseChairHandler extends AbstractMaplePacketHandler {

    @Override
    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient client) {
        final MapleCharacter player = client.getPlayer();
        int itemId = slea.readInt();
        if (player.getInventory(MapleInventoryType.SETUP).findById(itemId) == null) {
            return;
        }
        player.setChair(itemId);
        player.getMap().broadcastMessage(player, MaplePacketCreator.showChair(player.getId(), itemId), false);
        if (ArrayUtil.contains(player.getMapId(), MapleCharacter.FISHING_MAPS) && ArrayUtil.contains(player.getChair(), MapleCharacter.FISHING_CHAIRS)) {
            if (player.getFishingTask() == null || player.getFishingTask().isCanceled()) {
                player.runFishingTask();
                player.dropMessage(5, "You started fishing");
                player.dropMessage(5, "<Warning>If you do not have a slot in the ETC inventory, you will not be able to get the item. ");
                player.announce(MaplePacketCreator.earnTitleMessage("You started fishing"));
            }
        }
        client.announce(MaplePacketCreator.enableActions());
    }
}