package com.lucianms.events;

import com.lucianms.client.inventory.MapleInventoryType;
import com.lucianms.io.scripting.npc.NPCScriptManager;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.constants.ItemConstants;
import com.lucianms.events.PacketEvent;

/**
 * @author Generic
 * @author izarooni
 */
public class RemoteGachaponEvent extends PacketEvent {

    private int ticket;
    private int gacha;

    @Override
    public void processInput(MaplePacketReader reader) {
        ticket = reader.readInt();
        gacha = reader.readInt();
        if (ticket != 5451000) {
            setCanceled(true);
        } else if (gacha < 0 || gacha > 11) {
            setCanceled(true);
        }
        MapleInventoryType type = ItemConstants.getInventoryType(ticket);
        if (getClient().getPlayer().getInventory(type).countById(ticket) == 0) {
            setCanceled(true);
        }
    }

    @Override
    public Object onPacket() {
        int npcID = 9100100;
        if (gacha != 8 && gacha != 9) {
            npcID += gacha;
        } else {
            npcID = gacha == 8 ? 9100109 : 9100117;
        }
        NPCScriptManager.start(getClient(), npcID, "gachaponRemote");
        return null;
    }
}
