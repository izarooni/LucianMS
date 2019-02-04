package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.inventory.Item;
import com.lucianms.events.PacketEvent;
import com.lucianms.io.scripting.item.ItemScriptManager;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.MapleItemInformationProvider;
import com.lucianms.server.MapleItemInformationProvider.scriptedItem;
import tools.MaplePacketCreator;

/**
 * @author Jay Estrella
 * @author izarooni
 */
public class PlayerItemScriptedUseEvent extends PacketEvent {

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
        scriptedItem info = ii.getScriptedItemInfo(itemID);
        if (info == null) {
            return null;
        }
        ItemScriptManager ism = ItemScriptManager.getInstance();
        Item item = player.getInventory(ii.getInventoryType(itemID)).getItem(slot);
        if (item == null || item.getItemId() != itemID || item.getQuantity() < 1 || !ism.scriptExists(info.getScript())) {
            return null;
        }
        ism.getItemScript(getClient(), info.getScript());
        getClient().announce(MaplePacketCreator.enableActions());
        return null;
    }
}
