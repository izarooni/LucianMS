package com.lucianms.server.events;

import com.lucianms.nio.receive.MaplePacketReader;

public class PlayerUpdateEvent extends PacketEvent {

    @Override
    public boolean inValidState() {
        return true;
    }

    @Override
    public void processInput(MaplePacketReader reader) {
    }

    @Override
    public Object onPacket() {
        if (getClient().getPlayer() != null) {
            getClient().getPlayer().saveToDB();
        }
        return null;
    }
}
