package com.lucianms.events;

import com.lucianms.nio.receive.MaplePacketReader;

/**
 * @author izarooni
 */
public class WorldChannelSelectEvent extends PacketEvent {

    private byte world, channel;

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