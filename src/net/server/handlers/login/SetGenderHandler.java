package net.server.handlers.login;

import net.PacketEvent;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 * @author izarooni
 */
public class SetGenderHandler extends PacketEvent {

    private byte gender;

    @Override
    public void process(SeekableLittleEndianAccessor slea) {
        byte action = slea.readByte();
        if (action == 1 && getClient().getGender() != 10) {
            gender = slea.readByte();
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
