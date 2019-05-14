package com.lucianms.events;

import com.lucianms.client.LoginState;
import com.lucianms.nio.receive.MaplePacketReader;
import tools.MaplePacketCreator;

/**
 * @author izarooni
 */
public class AccountPINSetEvent extends PacketEvent {

    private byte action;
    private String PIN;

    @Override
    public void processInput(MaplePacketReader reader) {
        action = reader.readByte();
        if (action != 0) {
            PIN = reader.readMapleAsciiString();
        }
    }

    @Override
    public Object onPacket() {
        if (action == 0) {
            getClient().updateLoginState(LoginState.LogOut);
        } else {
            if (PIN.length() == 4) {
                getClient().setPin(PIN);
                getClient().announce(MaplePacketCreator.pinRegistered());
                getClient().updateLoginState(LoginState.LogOut);
            }
        }
        return null;
    }
}
