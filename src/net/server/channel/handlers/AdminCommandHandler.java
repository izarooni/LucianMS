package net.server.channel.handlers;

import net.PacketEvent;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 * @author izarooni
 */
public class AdminCommandHandler extends PacketEvent {

    @Override
    public void process(SeekableLittleEndianAccessor slea) {
        if (getClient().getPlayer().gmLevel() < 5) {
            return;
        }
        getLogger().info("Unhandled {}", slea.toString());
    }

    @Override
    public Object onPacket() {
        return null;
    }
}
