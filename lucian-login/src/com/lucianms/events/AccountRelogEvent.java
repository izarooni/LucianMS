package com.lucianms.events;

import com.lucianms.client.LoginState;
import com.lucianms.nio.receive.MaplePacketReader;
import tools.MaplePacketCreator;

/**
 * @author izarooni
 */
public class AccountRelogEvent extends PacketEvent {

    @Override
    public boolean inValidState() {
        return getClient().getLoginState() == LoginState.LogOut;
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