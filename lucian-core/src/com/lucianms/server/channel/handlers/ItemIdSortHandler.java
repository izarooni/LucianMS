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
package com.lucianms.server.channel.handlers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.lucianms.client.autoban.Cheater;
import com.lucianms.client.autoban.Cheats;
import com.lucianms.constants.ServerConstants;
import com.lucianms.events.PacketEvent;
import tools.MaplePacketCreator;
import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleClient;
import com.lucianms.client.inventory.Item;
import com.lucianms.client.inventory.MapleInventory;
import com.lucianms.client.inventory.MapleInventoryType;
import com.lucianms.client.inventory.ModifyInventory;
import tools.data.input.LittleEndianReader;

/**
 *
 * @author BubblesDev
 */
public final class ItemIdSortHandler extends PacketEvent {

    @Override
    public final void handlePacket(LittleEndianReader slea, MapleClient c) {
    	MapleCharacter chr = c.getPlayer();
        Cheater.CheatEntry entry = chr.getCheater().getCheatEntry(Cheats.FastInventorySort);
        if (System.currentTimeMillis() - entry.latestOperationTimestamp < 250) {
            entry.spamCount++;
            c.announce(MaplePacketCreator.enableActions());
            return;
        } else {
            entry.spamCount = 0;
        }
        entry.latestOperationTimestamp = System.currentTimeMillis();
        byte inventoryType = slea.readByte();
        
        if(!chr.isGM() || !ServerConstants.USE_ITEM_SORT) {
			c.announce(MaplePacketCreator.enableActions());
			return;
		}
		
		if (inventoryType < 1 || inventoryType > 5) {
            c.disconnect(false, false);
            return;
        }
		
        MapleInventory inventory = chr.getInventory(MapleInventoryType.getByType(inventoryType));
        ArrayList<Item> itemarray = new ArrayList<>();
        List<ModifyInventory> mods = new ArrayList<>();
        for (short i = 1; i <= inventory.getSlotLimit(); i++) {
            Item item = inventory.getItem(i);
            if (item != null) {
            	itemarray.add((Item) item.copy());
            }
        }
        
        Collections.sort(itemarray);
        for (Item item : itemarray) {
        	inventory.removeItem(item.getPosition());
        }
        
        for (Item item : itemarray) {
        	//short position = item.getPosition();
            inventory.addItem(item);
            if (inventory.getType().equals(MapleInventoryType.EQUIP)) {
	            mods.add(new ModifyInventory(3, item));
	            mods.add(new ModifyInventory(0, item.copy()));//to prevent crashes
	            //mods.add(new ModifyInventory(2, item.copy(), position));
            }
        }
        itemarray.clear();
        c.announce(MaplePacketCreator.modifyInventory(true, mods));
        c.announce(MaplePacketCreator.finishedSort2(inventoryType));
        c.announce(MaplePacketCreator.enableActions());
    }
}
