package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.inventory.MapleInventoryType;
import com.lucianms.constants.ItemConstants;
import com.lucianms.nio.receive.MaplePacketReader;
import tools.MaplePacketCreator;

/**
 * @author izarooni
 */
public class PlayerDeathItemUseEvent extends PacketEvent {

    private int itemID;

    @Override
    public void processInput(MaplePacketReader reader) {
        itemID = reader.readInt();
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        MapleInventoryType inventoryType = ItemConstants.getInventoryType(itemID);
        if (player.getInventory(inventoryType).findById(itemID) == null) {
            return null;
        }
        player.setItemEffect(itemID);
        getClient().announce(MaplePacketCreator.itemEffect(player.getId(), itemID));
        return null;
    }
}
