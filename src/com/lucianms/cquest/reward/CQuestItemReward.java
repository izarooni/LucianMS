package com.lucianms.cquest.reward;

import client.MapleCharacter;
import client.inventory.MapleInventoryType;
import constants.ItemConstants;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;

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
