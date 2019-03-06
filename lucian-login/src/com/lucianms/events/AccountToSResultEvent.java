package com.lucianms.events;

import com.lucianms.nio.receive.MaplePacketReader;
import tools.MaplePacketCreator;

/**
 * @author kevintjuh93
 */
public class AccountToSResultEvent extends PacketEvent {

    private boolean accepted;

    @Override
    public boolean inValidState() {
        return !getClient().isLoggedIn();
    }

    @Override
    public void processInput(MaplePacketReader reader) {
        accepted = reader.readByte() == 1;
    }

    @Override
    public Object onPacket() {
        if (accepted) {
            getClient().announce(MaplePacketCreator.getAuthSuccess(getClient()));
        } else {
            getClient().announce(MaplePacketCreator.getLoginFailed(9));
        }
        return null;
    }
}
