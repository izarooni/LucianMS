
package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.events.PacketEvent;
import com.lucianms.nio.receive.MaplePacketReader;
import tools.MaplePacketCreator;

/**
 * @author kevintjuh93
 * @author izarooni
 */
public class PlayerGachaponUsedEvent extends PacketEvent {

    @Override
    public void processInput(MaplePacketReader reader) {
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        if (player.getGachaExp() == 0) {
            return null;
        }
        player.gainGachaExp();
        getClient().announce(MaplePacketCreator.enableActions());
        return null;
    }
}
