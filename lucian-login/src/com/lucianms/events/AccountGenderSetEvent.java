package com.lucianms.events;

import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.events.PacketEvent;
import tools.MaplePacketCreator;

/**
 * @author izarooni
 */
public class AccountGenderSetEvent extends PacketEvent {

    private byte gender;

    @Override
    public void processInput(MaplePacketReader reader) {
        byte action = reader.readByte();
        if (action == 1 && getClient().getGender() != 10) {
            gender = reader.readByte();
        } else {
            setCanceled(true);
        }
    }

    @Override
    public Object onPacket() {
        getClient().setGender(gender);
        getClient().announce(MaplePacketCreator.getAuthSuccess(getClient()));
        return null;
    }
}
