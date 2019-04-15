package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.nio.receive.MaplePacketReader;

/**
 * @author izarooni
 */
public class PlayerDamageStatChangedEvent extends PacketEvent {
    @Override
    public void processInput(MaplePacketReader reader) {
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        player.checkBerserk();
        return null;
    }
}
