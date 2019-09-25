package com.lucianms.events;

import com.lucianms.nio.receive.MaplePacketReader;
import tools.MaplePacketCreator;

/**
 * @author izarooni
 */
public class NpcMoveEvent extends PacketEvent {

    private long available;
    private int objectID;
    private byte v3, v4;

    @Override
    public void processInput(MaplePacketReader reader) {
        available = reader.available();
        if (available == 6) {
            objectID = reader.readInt();
            v3 = reader.readByte();
            v4 = reader.readByte();
        }
//        else {
//            byte[] b = slea.read((int) (available - 9));
//            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
//            mplew.writeShort(SendOpcode.NPC_ACTION.getValue());
//            mplew.write(b);
//            getClient().announce(mplew.getPacket());
//        }
    }

    @Override
    public Object onPacket() {
        if (available == 6) {
            if (v3 == -1) {
                getClient().announce(MaplePacketCreator.getNpcChat(objectID, v4));
            } else {
                getClient().announce(MaplePacketCreator.getNpcAction(objectID, v3));
            }
        }
        return null;
    }
}
