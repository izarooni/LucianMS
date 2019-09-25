package com.lucianms.events;

import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.events.PacketEvent;
import com.lucianms.server.MapleItemInformationProvider;

/**
 * @author izarooni
 */
public class PlayerItemEffectCancelEvent extends PacketEvent {

    private int itemID;

    @Override
    public void processInput(MaplePacketReader reader) {
        itemID = -reader.readInt();
        if (MapleItemInformationProvider.getInstance().noCancelMouse(itemID)) {
            setCanceled(true);
        }
    }

    @Override
    public Object onPacket() {
        getClient().getPlayer().cancelEffect(itemID);
        return null;
    }
}