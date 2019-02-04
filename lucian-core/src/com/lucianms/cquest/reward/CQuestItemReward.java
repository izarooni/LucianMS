package com.lucianms.cquest.reward;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.inventory.MapleInventoryType;
import com.lucianms.constants.ItemConstants;
import com.lucianms.server.MapleInventoryManipulator;
import com.lucianms.server.MapleItemInformationProvider;

/**
 * @author izarooni
 */
public class CQuestItemReward implements CQuestReward {

    private final int itemId;
    private final short quantity;

    public CQuestItemReward(int itemId, short quantity) {
        this.itemId = itemId;
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return "CQuestItemReward{" + "itemId=" + itemId + ", quantity=" + quantity + '}';
    }

    @Override
    public boolean canAccept(MapleCharacter player) {
        MapleInventoryType iType = ItemConstants.getInventoryType(itemId);
        return !player.getInventory(iType).isFull() && MapleInventoryManipulator.checkSpace(player.getClient(), itemId, quantity, "");
    }

    @Override
    public void give(MapleCharacter player) {
        if (ItemConstants.getInventoryType(itemId) == MapleInventoryType.EQUIP) {
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            MapleInventoryManipulator.addFromDrop(player.getClient(), ii.getEquipById(itemId), true);
        } else {
            MapleInventoryManipulator.addById(player.getClient(), itemId, quantity);
        }
    }

    public int getItemId() {
        return itemId;
    }

    public short getQuantity() {
        return quantity;
    }
}
