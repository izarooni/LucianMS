package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleClient;
import com.lucianms.client.SpamTracker;
import com.lucianms.client.inventory.*;
import com.lucianms.nio.receive.MaplePacketReader;
import tools.Functions;
import tools.MaplePacketCreator;

import java.util.ArrayList;
import java.util.Collections;

/**
 * @author izarooni
 */
public class InventorySortItemsEvent extends PacketEvent {

    private byte inventoryType;

    @Override
    public void processInput(MaplePacketReader reader) {
        reader.readInt();
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
        if (spamTracker.testFor(500)) {
            client.announce(MaplePacketCreator.enableActions());
            return null;
        }
        spamTracker.record();

        if (inventoryType < 1 || inventoryType > 5) {
            return null;
        }

        for (MaplePet pet : player.getPets()) {
            // crashes if you don't
            Functions.requireNotNull(pet, p -> player.unequipPet(p, false));
        }

        MapleInventory inventory = player.getInventory(MapleInventoryType.getByType(inventoryType));
        ArrayList<Item> sortedItems = new ArrayList<>(inventory.list());
        ArrayList<ModifyInventory> mods = new ArrayList<>(sortedItems.size());

        //region remove items visually
        for (Item item : sortedItems) {
            mods.add(new ModifyInventory(3, item));
            inventory.removeSlot(item.getPosition());
        }
        client.announce(MaplePacketCreator.modifyInventory(false, mods));
        mods.clear();
        //endregion

        //region sort and re-insert items
        Collections.sort(sortedItems);
        for (Item item : sortedItems) {
            inventory.addItem(item);
            mods.add(new ModifyInventory(0, item));
        }
        client.announce(MaplePacketCreator.modifyInventory(true, mods));
        //endregion

        sortedItems.clear();
        mods.clear();

        client.announce(MaplePacketCreator.getInventorySortItems(inventoryType));
        return null;
    }
}
