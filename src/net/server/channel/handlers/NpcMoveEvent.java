package net.server.channel.handlers;

import net.PacketEvent;
import net.SendOpcode;
import tools.data.input.SeekableLittleEndianAccessor;
import tools.data.output.MaplePacketLittleEndianWriter;

/**
 * @author izarooni
 */
public class NpcMoveEvent extends PacketEvent {

    public static byte[] getNpcChat(int objectID, byte chat) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.NPC_ACTION.getValue());
        mplew.writeInt(objectID);
        mplew.write(-1);
        mplew.write(chat);
        return mplew.getPacket();
    }

    public static byte[] getNpcAction(int objectID, byte action) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendOpcode.NPC_ACTION.getValue());
        mplew.writeInt(objectID);
        mplew.write(action);
        mplew.write(0xFF);
        return mplew.getPacket();
    }

    private long available;
    private int objectID;
    private byte v3, v4;

    @Override
    public void process(SeekableLittleEndianAccessor slea) {
        available = slea.available();
        if (available == 6) {
            objectID = slea.readInt();
            v3 = slea.readByte();
            v4 = slea.readByte();
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
                getClient().announce(getNpcChat(objectID, v4));
            } else {
                getClient().announce(getNpcAction(objectID, v3));
            }
        }
        return null;
    }
}
