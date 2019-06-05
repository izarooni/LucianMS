package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.constants.ItemConstants;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.MakerItemFactory;
import com.lucianms.server.MakerItemFactory.MakerItemCreateEntry;
import com.lucianms.server.MapleInventoryManipulator;
import com.lucianms.server.MapleItemInformationProvider;
import tools.Pair;

/**
 * @author Jay Estrella
 * @author izarooni
 */
public class PlayerSkillItemMakerEvent extends PacketEvent {

    private static final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();

    private int itemID;

    @Override
    public void processInput(MaplePacketReader reader) {
        reader.readInt();
        itemID = reader.readInt();
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        MakerItemCreateEntry recipe = MakerItemFactory.getItemCreateEntry(itemID);
        if (canCreate(player, recipe) && !player.getInventory(ItemConstants.getInventoryType(itemID)).isFull()) {
            for (Pair<Integer, Integer> p : recipe.getReqItems()) {
                int toRemove = p.getLeft();
                MapleInventoryManipulator.removeById(getClient(), ItemConstants.getInventoryType(toRemove), toRemove, p.getRight(), false, false);
            }
            MapleInventoryManipulator.addById(getClient(), itemID, (short) recipe.getRewardAmount());
        }
        return null;
    }

    private static boolean canCreate(MapleCharacter player, MakerItemCreateEntry recipe) {
        return hasItems(player, recipe) && player.getMeso() >= recipe.getCost() && player.getLevel() >= recipe.getReqLevel() && player.getSkillLevel(player.getJob().getId() / 1000 * 1000 + 1007) >= recipe.getReqSkillLevel();
    }

    private static boolean hasItems(MapleCharacter player, MakerItemCreateEntry recipe) {
        for (Pair<Integer, Integer> p : recipe.getReqItems()) {
            int itemId = p.getLeft();
            if (player.getInventory(ItemConstants.getInventoryType(itemId)).countById(itemId) < p.getRight()) {
                return false;
            }
        }
        return true;
    }
}
