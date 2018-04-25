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
import client.autoban.Cheater;
import client.autoban.Cheats;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import net.PacketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scripting.npc.NPCScriptManager;
import server.MapleInventoryManipulator;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 * @author Matze
 */
public class ItemMoveHandler extends PacketHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ItemMoveHandler.class);

    private MapleInventoryType inventoryType;
    private short source;
    private short action;
    private short quantity;

    @Override
    public void process(SeekableLittleEndianAccessor slea) {
        slea.skip(4);
        inventoryType = MapleInventoryType.getByType(slea.readByte());
        source = slea.readShort();
        action = slea.readShort();
        quantity = slea.readShort();

        if (source < 0 && action > 0 && source == -149) {
            NPCScriptManager.start(getClient(), 9010000, "f_equip_info");
            setCanceled(true);
        }
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        Cheater.CheatEntry entry = player.getCheater().getCheatEntry(Cheats.FastInventorySort);

        if (System.currentTimeMillis() - entry.latestOperationTimestamp < 180) {
            entry.spamCount++;
            getClient().announce(MaplePacketCreator.enableActions());
            player.dropMessage("You are doing that too fast");
            return null;
        } else {
            entry.spamCount = 0;
            entry.latestOperationTimestamp = System.currentTimeMillis();
        }


        Item item;
        if (source < 0 && action > 0) {
            item = player.getInventory(MapleInventoryType.EQUIPPED).getItem(source);
            MapleInventoryManipulator.unequip(getClient(), source, action);
            return item;
        } else if (action < 0) {
            item = player.getInventory(MapleInventoryType.EQUIP).getItem(source);
            MapleInventoryManipulator.equip(getClient(), source, action);
            return item;
        } else if (action == 0) {
            return MapleInventoryManipulator.drop(getClient(), inventoryType, source, quantity);
        } else {
            item = player.getInventory(inventoryType).getItem(source);
            MapleInventoryManipulator.move(getClient(), inventoryType, source, action);
            return item;
        }
    }

    public MapleInventoryType getInventoryType() {
        return inventoryType;
    }

    public short getSource() {
        return source;
    }

    public short getAction() {
        return action;
    }

    public short getQuantity() {
        return quantity;
    }
}