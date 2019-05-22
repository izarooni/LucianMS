package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleClient;
import com.lucianms.client.SpamTracker;
import com.lucianms.client.inventory.MapleInventory;
import com.lucianms.client.inventory.MapleInventoryType;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.MapleInventoryManipulator;
import tools.MaplePacketCreator;

/**
 * @author izarooni
 */
public class InventoryGatherItemsEvent extends PacketEvent {

    private byte inventoryType;

    @Override
    public void processInput(MaplePacketReader reader) {
        reader.skip(4);
        inventoryType = reader.readByte();
    }

    @Override
    public Object onPacket() {
        MapleClient client = getClient();
        MapleCharacter player = client.getPlayer();
        if (player.getCashShop().isOpened()
                || player.getStorage().isOpened()
                || player.getTrade() != null
                || player.getMiniGame() != null) {
            client.announce(MaplePacketCreator.enableActions());
            return null;
        }

        SpamTracker.SpamData spamTracker = player.getSpamTracker(SpamTracker.SpamOperation.InventorySort);
        if (spamTracker.testFor(100)) {
            return null;
        }
        spamTracker.record();
        if (inventoryType < 1 || inventoryType > 5) return null;
        MapleInventoryType iType = MapleInventoryType.values()[inventoryType];
        MapleInventory inventory = player.getInventory(iType);
        if (inventory.isFull()) return null;

        while (true) {
            short freeSlot = inventory.getNextFreeSlot();
            if (freeSlot == -1) break;

            short itemSlot = -1;
            for (short i = (short) (freeSlot + 1); i <= inventory.getSlotLimit(); i++) {
                if (inventory.getItem(i) != null) {
                    itemSlot = i;
                    break;
                }
            }
            if (itemSlot > 0) {
                MapleInventoryManipulator.move(client, iType, itemSlot, freeSlot);
            } else {
                break;
            }
        }
        client.announce(MaplePacketCreator.getInventoryGatherItems(iType.getType()));
        return null;
    }
}