package com.lucianms.events;

import com.lucianms.nio.receive.MaplePacketReader;
import tools.MaplePacketCreator;

/**
 * @author Jay Estrella
 * @author izarooni
 */
public class PlayerLifeUseEvent extends PacketEvent {

    private String name;

    @Override
    public void processInput(MaplePacketReader reader) {
        name = reader.readMapleAsciiString();
    }

    @Override
    public Object onPacket() {
        getClient().announce(MaplePacketCreator.charNameResponse(name, false));
        return null;
    }
}
