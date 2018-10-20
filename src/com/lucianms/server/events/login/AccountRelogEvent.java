package com.lucianms.server.events.login;

import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.events.PacketEvent;
import tools.MaplePacketCreator;

/**
 * @author izarooni
 */
public class AccountRelogEvent extends PacketEvent {

    @Override
    public boolean inValidState() {
        return !getClient().isLoggedIn();
    }

    @Override
    public void processInput(MaplePacketReader reader) {
    }

    @Override
    public Object onPacket() {
        getClient().announce(MaplePacketCreator.getRelogResponse());
        return null;
    }
}