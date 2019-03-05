package com.lucianms.events;

import com.lucianms.nio.receive.MaplePacketReader;

public class FieldSetEvent extends PacketEvent {

    @Override
    public boolean inValidState() {
        return getClient().isLoggedIn() && getClient().getPlayer() != null;
    }

    @Override
    public void processInput(MaplePacketReader reader) {
    }

    @Override
    public Object onPacket() {
        getClient().getPlayer().setRates();
        return null;
    }
}
