package com.lucianms.events;

import com.lucianms.client.MapleClient;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.events.PacketEvent;
import tools.MaplePacketCreator;

/**
 * @author izarooni
 */
public class AccountPostLoginEvent extends PacketEvent {

    private byte c2;
    private byte c3 = 5;
    private String pin;

    @Override
    public void processInput(MaplePacketReader reader) {
        c2 = reader.readByte();
        if (reader.available() > 0) {
            c3 = reader.readByte();
        }
        if ((c2 == 1 || c2 == 2) && c3 == 0) {
            pin = reader.readMapleAsciiString();
        }
    }

    @Override
    public Object onPacket() {
        if (c2 == 1 && c3 == 1) {
            getClient().announce(getClient().getPic() == null ? MaplePacketCreator.registerPin() : MaplePacketCreator.requestPin());
        } else if (c2 == 1 && c3 == 0) {
            getClient().announce(getClient().checkPin(pin) ? MaplePacketCreator.pinAccepted() : MaplePacketCreator.requestPinAfterFailure());
        } else if (c2 == 2 && c3 == 0) {
            getClient().announce(getClient().checkPin(pin) ? MaplePacketCreator.registerPin() : MaplePacketCreator.requestPinAfterFailure());
        } else if (c2 == 0 && c3 == 5) {
            getClient().updateLoginState(MapleClient.LOGIN_NOTLOGGEDIN);
        }
        return null;
    }
}
