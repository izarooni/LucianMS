package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.inventory.Item;
import com.lucianms.client.inventory.MapleInventoryType;
import com.lucianms.constants.ItemConstants;
import com.lucianms.nio.receive.MaplePacketReader;
import tools.MaplePacketCreator;

/**
 * @author Kevin
 * @author izarooni
 */
public class PlayerWeddingEvent extends PacketEvent {

    private byte action;
    private short slot;
    private short quantity;
    private int itemID;

    @Override
    public void processInput(MaplePacketReader reader) {
        action = reader.readByte();
        switch (action) {
            case 6:
                slot = reader.readShort();
                itemID = reader.readInt();
                quantity = reader.readShort();
                break;
        }
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        switch (action) {
            case 6: // Add an item to the Wedding Registry
                MapleInventoryType type = ItemConstants.getInventoryType(itemID);
                Item item = player.getInventory(type).getItem(slot);
                if (itemID == item.getItemId() && quantity <= item.getQuantity()) {
                    getClient().announce(MaplePacketCreator.addItemToWeddingRegistry(player, item));
                }
                break;
        }
        return null;
    }
}
