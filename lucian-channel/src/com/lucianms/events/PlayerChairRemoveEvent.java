package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.events.PacketEvent;
import tools.MaplePacketCreator;

/**
 * @author izarooni
 */
public class PlayerChairRemoveEvent extends PacketEvent {

    private int chairID;

    @Override
    public void processInput(MaplePacketReader reader) {
        chairID = reader.readInt();
    }

    @Override
    public Object onPacket() {
        final MapleCharacter player = getClient().getPlayer();

        if (chairID == -1) { // Cancel Chair
            player.setChair(0);
            getClient().announce(MaplePacketCreator.cancelChair(-1));
            player.getMap().broadcastMessage(player, MaplePacketCreator.showChair(player.getId(), 0), false);
        } else { // Use In-Map Chair
            player.setChair(chairID);
            getClient().announce(MaplePacketCreator.cancelChair(chairID));
        }
        if (player.getFishingTask() != null && !player.getFishingTask().isCanceled()) {
            player.getFishingTask().cancel();
            player.dropMessage(5, "You stopped fishing");
            player.announce(MaplePacketCreator.earnTitleMessage("You stopped fishing"));
        }
        return null;
    }
}

