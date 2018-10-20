package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.inventory.MapleInventoryType;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.events.PacketEvent;
import tools.ArrayUtil;
import tools.MaplePacketCreator;

/**
 * @author izarooni
 */
public class PlayerChairUseEvent extends PacketEvent {

    private int itemID;

    @Override
    public void processInput(MaplePacketReader reader) {
        itemID = reader.readInt();
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        if (player.getInventory(MapleInventoryType.SETUP).findById(itemID) == null) {
            return null;
        }
        player.setChair(itemID);
        player.getMap().broadcastMessage(player, MaplePacketCreator.showChair(player.getId(), itemID), false);
        if (ArrayUtil.contains(player.getMapId(), MapleCharacter.FISHING_MAPS) && ArrayUtil.contains(player.getChair(), MapleCharacter.FISHING_CHAIRS)) {
            if (player.getFishingTask() == null || player.getFishingTask().isCanceled()) {
                player.runFishingTask();
                player.dropMessage(5, "You started fishing");
                player.dropMessage(5, "<Warning>If you do not have a slot in the ETC inventory, you will not be able to get the item. ");
                player.announce(MaplePacketCreator.earnTitleMessage("You started fishing"));
            }
        }
        getClient().announce(MaplePacketCreator.enableActions());
        return null;
    }
}