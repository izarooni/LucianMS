package com.lucianms.server.events;

import net.PacketEvent;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 * @author izarooni
 */
public class PongEvent extends PacketEvent {

    @Override
    public boolean inValidState() {
        return true;
    }

    @Override
    public void process(SeekableLittleEndianAccessor slea) {
    }

    @Override
    public Object onPacket() {
        getClient().pongReceived();
        return null;
    }
}
