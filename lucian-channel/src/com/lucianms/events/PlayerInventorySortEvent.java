package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.SpamTracker;
import com.lucianms.client.inventory.MapleInventory;
import com.lucianms.client.inventory.MapleInventoryType;
import com.lucianms.constants.ServerConstants;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.MapleInventoryManipulator;
import tools.MaplePacketCreator;

/**
 * @author izarooni
 */
public class PlayerInventorySortEvent extends PacketEvent {

    private byte inventoryType;

    @Override
    public void processInput(MaplePacketReader reader) {
        reader.skip(4);
        inventoryType = reader.readByte();
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        SpamTracker.SpamData spamTracker = player.getSpamTracker(SpamTracker.SpamOperation.InventorySort);
        if (spamTracker.testFor(100)) {
            getClient().announce(MaplePacketCreator.enableActions());
            return null;
        }
        spamTracker.record();

        MapleInventoryType inventoryType = MapleInventoryType.getByType(this.inventoryType);
        if (inventoryType == null || inventoryType == MapleInventoryType.UNDEFINED || player.getInventory(inventoryType).isFull()) {
            getClient().announce(MaplePacketCreator.enableActions());
            return null;
        }
        if (!player.isGM() && !ServerConstants.USE_ITEM_SORT) {
            getClient().announce(MaplePacketCreator.enableActions());
            return null;
        }
        MapleInventory inventory = player.getInventory(inventoryType);

        while (true) {
            short freeSlot = inventory.getNextFreeSlot();
            if (freeSlot == -1) {
                break;
            } else {
                short itemSlot = -1;
                for (short i = (short) (freeSlot + 1); i <= inventory.getSlotLimit(); i++) {
                    if (inventory.getItem(i) != null) {
                        itemSlot = i;
                        break;
                    }
                }
                if (itemSlot > 0) {
                    MapleInventoryManipulator.move(getClient(), inventoryType, itemSlot, freeSlot);
                } else {
                    break;
                }
            }
        }
        getClient().announce(MaplePacketCreator.finishedSort(inventoryType.getType()));
        getClient().announce(MaplePacketCreator.enableActions());
        return null;
    }
}