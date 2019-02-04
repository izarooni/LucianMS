
package com.lucianms.events;

import com.lucianms.nio.receive.MaplePacketReader;
import tools.MaplePacketCreator;

/**
 * @author kevintjuh93
 * @author izarooni
 */
public class PlayerLeftKnockbackEvent extends PacketEvent {

    @Override
    public void processInput(MaplePacketReader reader) {
    }

    @Override
    public Object onPacket() {
        getClient().announce(MaplePacketCreator.leftKnockBack());
        getClient().announce(MaplePacketCreator.enableActions());
        return null;
    }
}
