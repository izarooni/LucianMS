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
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class UseChairHandler extends AbstractMaplePacketHandler {
    @Override
    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        int itemId = slea.readInt();
        if (c.getPlayer().getInventory(MapleInventoryType.SETUP).findById(itemId) == null) {
            return;
        }
        c.getPlayer().setChair(itemId);
        c.getPlayer().getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.showChair(c.getPlayer().getId(), itemId), false);
        c.getPlayer();
		c.getPlayer();
		if(c.getPlayer().in_array(MapleCharacter.FISHING_MAPS, c.getPlayer().getMapId()) && c.getPlayer().in_array(MapleCharacter.FISHING_CHAIRS, c.getPlayer().getChair())) {
        	if(c.getPlayer().getFishingTask() == null || c.getPlayer().getFishingTask().isCancelled() || c.getPlayer().getFishingTask().isDone()) {
        		c.getPlayer().runFishingTask();
        		c.getPlayer().dropMessage(5, "You started fishing");
        		c.getPlayer().dropMessage(5, "<Warning>If you do not have a slot in the ETC inventory, you will not be able to get the item. ");
        		c.getPlayer().announce(MaplePacketCreator.earnTitleMessage("You started fishing"));
        	}
        }
        c.announce(MaplePacketCreator.enableActions());
    }
}