package com.lucianms.events;

import com.lucianms.client.MapleClient;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.events.PacketEvent;
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
            getClient().updateLoginState(MapleClient.LOGIN_NOTLOGGEDIN);
        } else {
            if (PIN.length() == 4) {
                getClient().setPin(PIN);
                getClient().announce(MaplePacketCreator.pinRegistered());
                getClient().updateLoginState(MapleClient.LOGIN_NOTLOGGEDIN);
            }
        }
        return null;
    }
}
