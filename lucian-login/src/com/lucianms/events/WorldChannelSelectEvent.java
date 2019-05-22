package com.lucianms.events;

import com.lucianms.nio.SendOpcode;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.nio.send.MaplePacketWriter;
import tools.MaplePacketCreator;

/**
 * @author izarooni
 */
public class WorldChannelSelectEvent extends PacketEvent {

    private static byte[] getErrorResponse(byte action) {
        MaplePacketWriter writer = new MaplePacketWriter(3);
        writer.writeShort(SendOpcode.CHARLIST.getValue());
        writer.write(action);
        return writer.getPacket();
    }

    private byte world, channel;

    @Override
    public boolean exceptionCaught(Throwable t) {
        getClient().announce(getErrorResponse((byte) 6));
        getClient().announce(MaplePacketCreator.serverNotice(1, "An error occurred trying to retrieve one or more of your characters.\r\nFor your safety, logging into this world has been prevented."));
        return super.exceptionCaught(t);
    }

    @Override
    public void processInput(MaplePacketReader reader) {
        reader.readByte();
        world = reader.readByte();
        channel = reader.readByte();
    }

    @Override
    public Object onPacket() {
        getClient().setWorld(world);
        getClient().setChannel(channel + 1);
        getClient().sendCharList(world);
        return null;
    }
}