package com.lucianms.events;

import com.lucianms.nio.receive.MaplePacketReader;

/**
 * @author izarooni
 */
public class IgnoredPacketEvent extends PacketEvent {
    @Override
    public void processInput(MaplePacketReader reader) {
    }

    @Override
    public Object onPacket() {
        return null;
    }
}
