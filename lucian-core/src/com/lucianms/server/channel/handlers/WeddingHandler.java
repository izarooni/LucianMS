/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lucianms.server.channel.handlers;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleClient;
import com.lucianms.client.inventory.Item;
import com.lucianms.client.inventory.MapleInventoryType;
import com.lucianms.events.PacketEvent;
import com.lucianms.server.MapleItemInformationProvider;
import tools.MaplePacketCreator;
import tools.data.input.LittleEndianReader;

/**
 *
 * @author Kevin
 */
public class WeddingHandler extends PacketEvent {

    @Override
    public void handlePacket(LittleEndianReader slea, MapleClient c) {
        //System.out.println("Wedding Packet: " + slea);
        MapleCharacter chr = c.getPlayer();
        byte operation = slea.readByte();
        switch (operation) {
            case 0x06://Add an item to the Wedding Registry
                short slot = slea.readShort();
                int itemid = slea.readInt();
                short quantity = slea.readShort();
                MapleInventoryType type = MapleItemInformationProvider.getInstance().getInventoryType(itemid);
                Item item = chr.getInventory(type).getItem(slot);
                if (itemid == item.getItemId() && quantity <= item.getQuantity()) {
                    c.announce(MaplePacketCreator.addItemToWeddingRegistry(chr, item));
                }
        }
    }
}
