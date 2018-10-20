package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.inventory.Item;
import com.lucianms.client.inventory.MapleInventoryType;
import com.lucianms.events.PacketEvent;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.MapleInventoryManipulator;
import com.lucianms.server.MapleItemInformationProvider;
import tools.MaplePacketCreator;

/**
 * @author XoticStory
 * @author kevintjuh93
 * @author izarooni
 */
public class PlayerSolomonUseEVent extends PacketEvent {

    private static final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();

    private short slot;
    private int itemID;

    @Override
    public void processInput(MaplePacketReader reader) {
        reader.readInt();
        slot = reader.readShort();
        itemID = reader.readInt();
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        Item slotItem = player.getInventory(MapleInventoryType.USE).getItem(slot);
        int gachaexp = ii.getExpById(itemID);
        if (player.getInventory(MapleInventoryType.USE).countById(itemID) <= 0 || slotItem.getItemId() != itemID || player.getLevel() > ii.getMaxLevelById(itemID)) {
            return null;
        }
        if ((player.getGachaExp() + gachaexp) < 0) { // integer overflow
            return null;
        }
        player.gainGachaExp(gachaexp);
        MapleInventoryManipulator.removeFromSlot(getClient(), MapleInventoryType.USE, slot, (short) 1, false);
        getClient().announce(MaplePacketCreator.enableActions());
        return null;
    }
}
