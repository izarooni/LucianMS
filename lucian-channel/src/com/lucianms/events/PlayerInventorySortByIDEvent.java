package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.SpamTracker;
import com.lucianms.client.inventory.Item;
import com.lucianms.client.inventory.MapleInventory;
import com.lucianms.client.inventory.MapleInventoryType;
import com.lucianms.client.inventory.ModifyInventory;
import com.lucianms.constants.ServerConstants;
import com.lucianms.nio.receive.MaplePacketReader;
import tools.MaplePacketCreator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author BubblesDev
 * @author izarooni
 */
public class PlayerInventorySortByIDEvent extends PacketEvent {

    private byte inventoryType;

    @Override
    public void processInput(MaplePacketReader reader) {
        inventoryType = reader.readByte();
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        SpamTracker.SpamData spamTracker = player.getSpamTracker(SpamTracker.SpamOperation.InventorySort);
        if (spamTracker.testFor(500)) {
            getClient().announce(MaplePacketCreator.enableActions());
            return null;
        }
        spamTracker.record();
        if (!player.isGM() || !ServerConstants.USE_ITEM_SORT) {
            getClient().announce(MaplePacketCreator.enableActions());
            return null;
        }

        if (inventoryType < 1 || inventoryType > 5) {
            getClient().disconnect(false);
            return null;
        }

        MapleInventory inventory = player.getInventory(MapleInventoryType.getByType(inventoryType));
        ArrayList<Item> itemarray = new ArrayList<>();
        List<ModifyInventory> mods = new ArrayList<>();
        for (short i = 1; i <= inventory.getSlotLimit(); i++) {
            Item item = inventory.getItem(i);
            if (item != null) {
                itemarray.add(item.copy());
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
        getClient().announce(MaplePacketCreator.modifyInventory(true, mods));
        getClient().announce(MaplePacketCreator.finishedSort2(inventoryType));
        getClient().announce(MaplePacketCreator.enableActions());
        return null;
    }
}
