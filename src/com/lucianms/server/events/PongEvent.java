package com.lucianms.server.events;

import com.lucianms.nio.receive.MaplePacketReader;

/**
 * @author izarooni
 */
public class PongEvent extends PacketEvent {

    @Override
    public boolean inValidState() {
        return true;
    }

    @Override
    public void processInput(MaplePacketReader reader) {
    }

    @Override
    public Object onPacket() {
        getClient().pongReceived();
        return null;
    }
}
