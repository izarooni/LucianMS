package com.lucianms.server.events.login;

import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.events.PacketEvent;
import tools.MaplePacketCreator;

/**
 * @author izarooni
 */
public class AccountPlayerDeleteEvent extends PacketEvent {

    private String PIC;
    private int playerID;

    @Override
    public void processInput(MaplePacketReader reader) {
        PIC = reader.readMapleAsciiString();
        playerID = reader.readInt();
    }

    @Override
    public Object onPacket() {
        if (getClient().checkPic(PIC)) {
            getClient().announce(MaplePacketCreator.deleteCharResponse(playerID, 0));
            getClient().deleteCharacter(playerID);
        } else {
            getClient().announce(MaplePacketCreator.deleteCharResponse(playerID, 0x14));
        }
        return null;
    }
}