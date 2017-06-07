package net.server.channel.handlers;

import client.MapleCharacter;
import client.autoban.Cheater;
import client.autoban.Cheats;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import constants.ServerConstants;
import net.PacketHandler;
import server.MapleInventoryManipulator;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 * @author izarooni
 */
public class ItemSortHandler extends PacketHandler {

    private byte inventoryType;

    @Override
    public void process(SeekableLittleEndianAccessor slea) {
        slea.skip(4);
        inventoryType = slea.readByte();
    }

    @Override
    public void onPacket() {
        MapleCharacter player = getClient().getPlayer();
        Cheater.CheatEntry cheat = player.getCheater().getCheatEntry(Cheats.FastInventorySort);

        if (System.currentTimeMillis() - cheat.latestOperationTimestamp < 300) {
            cheat.spamCount++;
            getClient().announce(MaplePacketCreator.enableActions());
            return;
        } else {
            cheat.spamCount = 0;
        }
        cheat.latestOperationTimestamp = System.currentTimeMillis();

        MapleInventoryType inventoryType = MapleInventoryType.getByType(this.inventoryType);
        if (inventoryType == null || inventoryType == MapleInventoryType.UNDEFINED || player.getInventory(inventoryType).isFull()) {
            getClient().announce(MaplePacketCreator.enableActions());
            return;
        }
        if (!player.isGM() && !ServerConstants.USE_ITEM_SORT) {
            getClient().announce(MaplePacketCreator.enableActions());
            return;
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
    }
}