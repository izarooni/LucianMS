package net.server.channel.handlers;

import client.MapleCharacter;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.events.PacketEvent;
import tools.MaplePacketCreator;

/**
 * @author Xterminator
 * @author izarooni
 */
public class PlayerChalkboardCloseEvent extends PacketEvent {

    @Override
    public void processInput(MaplePacketReader reader) {

    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();

        player.setChalkboard(null);
        player.getMap().broadcastMessage(MaplePacketCreator.useChalkboard(player, true));
        return null;
    }
}
