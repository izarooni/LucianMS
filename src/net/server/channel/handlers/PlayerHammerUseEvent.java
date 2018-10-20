package net.server.channel.handlers;

import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.events.PacketEvent;
import tools.MaplePacketCreator;

/**
 * @author Jay Estrella
 * @author izarooni
 */
public class PlayerHammerUseEvent extends PacketEvent {

    @Override
    public void processInput(MaplePacketReader reader) {
    }

    @Override
    public Object onPacket() {
        getClient().announce(MaplePacketCreator.sendHammerMessage());
        return null;
    }
}
