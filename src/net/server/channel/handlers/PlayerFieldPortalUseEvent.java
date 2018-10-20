package net.server.channel.handlers;

import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.events.PacketEvent;

/**
 * @author izarooni
 */
public class PlayerFieldPortalUseEvent extends PacketEvent {

    @Override
    public void processInput(MaplePacketReader reader) {
    }

    @Override
    public Object onPacket() {
        return null;
    }
}