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
import client.inventory.Item;
import client.inventory.ItemFactory;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import net.PacketEvent;
import net.server.Server;
import server.MapleInventoryManipulator;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.data.input.LittleEndianReader;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * @author kevintjuh93
 */
public class FredrickHandler extends PacketEvent {

    @Override
    public void handlePacket(LittleEndianReader slea, MapleClient c) {
        MapleCharacter chr = c.getPlayer();
        byte operation = slea.readByte();

        switch (operation) {
            case 0x19: //Will never come...
                //c.announce(MaplePacketCreator.getFredrick((byte) 0x24));
                break;
            case 0x1A:
                List<Pair<Item, MapleInventoryType>> items;
                try (Connection con = c.getChannelServer().getConnection()) {
                    items = ItemFactory.MERCHANT.loadItems(con, chr.getId(), false);
                    if (!check(chr, items)) {
                        c.announce(MaplePacketCreator.fredrickMessage((byte) 0x21));
                        return;
                    }
                    chr.gainMeso(chr.getMerchantMeso(), false);
                    chr.setMerchantMeso(0);
                    if (deleteItems(chr)) {
                        if (chr.getHiredMerchant() != null)
                            chr.getHiredMerchant().clearItems();

                        for (Pair<Item, MapleInventoryType> p : items) {
                            Item left = p.getLeft();
                            MapleInventoryManipulator.addFromDrop(c, left, false);
                            Server.insertLog(getClass().getSimpleName(), "{} gained {} of {}", chr.getName(), left.getQuantity(), left.getItemId());
                        }
                        c.announce(MaplePacketCreator.fredrickMessage((byte) 0x1E));

                    } else {
                        chr.message("An unknown error has occured.");
                    }
                    break;
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
                break;
            case 0x1C: //Exit
                break;
            default:
        }
    }

    private static boolean check(MapleCharacter chr, List<Pair<Item, MapleInventoryType>> items) {
        if (chr.getMeso() + chr.getMerchantMeso() < 0) {
            return false;
        }

        if (!MapleInventory.checkSpots(chr, items)) {
            return false;
        }

        return true;
    }

    private static boolean deleteItems(MapleCharacter chr) {
        try (Connection con = chr.getClient().getChannelServer().getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM `inventoryitems` WHERE `type` = ? AND `characterid` = ?")) {
            ps.setInt(1, ItemFactory.MERCHANT.getValue());
            ps.setInt(2, chr.getId());
            ps.execute();
            return true;
        } catch (SQLException e) {
            return false;
        }

    }
}
