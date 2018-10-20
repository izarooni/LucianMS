package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.inventory.Item;
import com.lucianms.client.inventory.MapleInventoryType;
import com.lucianms.events.PacketEvent;
import com.lucianms.nio.receive.MaplePacketReader;
import tools.MaplePacketCreator;

/**
 * @author izarooni
 */
public class PlayerItemEffectUseEvent extends PacketEvent {

    private int itemID;

    @Override
    public void processInput(MaplePacketReader reader) {
        itemID = reader.readInt();
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();

        Item item;
        if (itemID == 4290001 || itemID == 4290000) {
            item = player.getInventory(MapleInventoryType.ETC).findById(itemID);
        } else {
            item = player.getInventory(MapleInventoryType.CASH).findById(itemID);
        }
        if (item == null || item.getQuantity() < 1) {
            if (itemID != 0) {
                return null;
            }
        }
        player.setItemEffect(itemID);
        player.getMap().broadcastMessage(player, MaplePacketCreator.itemEffect(player.getId(), itemID), false);
        return null;
    }
}
