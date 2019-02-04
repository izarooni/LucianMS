package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.events.PacketEvent;
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
