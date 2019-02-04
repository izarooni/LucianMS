package com.lucianms.events;

import com.lucianms.events.PacketEvent;
import com.lucianms.nio.receive.MaplePacketReader;
import tools.MaplePacketCreator;

/**
 * @author izarooni
 */
public class PlayerMonsterBookCoverEvent extends PacketEvent {

    private int ID;

    @Override
    public void processInput(MaplePacketReader reader) {
        ID = reader.readInt();
    }

    @Override
    public Object onPacket() {
        if (ID == 0 || ID / 10000 == 238) {
            getClient().getPlayer().setMonsterBookCover(ID);
            getClient().announce(MaplePacketCreator.changeCover(ID));
        }
        return null;
    }
}
