package net.server.channel.handlers;

import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.events.PacketEvent;

/**
 * @author izarooni
 */
public class AdministratorCommandEvent extends PacketEvent {

    @Override
    public void processInput(MaplePacketReader reader) {
        if (getClient().getPlayer().gmLevel() < 5) {
            return;
        }
        getLogger().info("Unhandled {}", reader.toString());
    }

    @Override
    public Object onPacket() {
        return null;
    }
}
