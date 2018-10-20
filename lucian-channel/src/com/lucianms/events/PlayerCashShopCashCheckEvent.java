package com.lucianms.events;

import com.lucianms.nio.receive.MaplePacketReader;
import tools.MaplePacketCreator;

/**
 * @author Acrylic (Terry Han)
 * @author izarooni
 */
public final class PlayerCashShopCashCheckEvent extends PacketEvent {

    @Override
    public void processInput(MaplePacketReader reader) {
    }

    @Override
    public Object onPacket() {
        getClient().announce(MaplePacketCreator.showCash(getClient().getPlayer()));
        return null;
    }
}
