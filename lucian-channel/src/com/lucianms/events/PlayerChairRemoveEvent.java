package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.nio.receive.MaplePacketReader;
import tools.MaplePacketCreator;

/**
 * @author izarooni
 */
public class PlayerChairRemoveEvent extends PacketEvent {

    private int chairID = -1;

    @Override
    public void processInput(MaplePacketReader reader) {
        chairID = reader.readShort();
    }

    @Override
    public Object onPacket() {
        final MapleCharacter player = getClient().getPlayer();

        player.setChair(chairID);
        getClient().announce(MaplePacketCreator.cancelChair(chairID));
        player.getMap().broadcastMessage(player, MaplePacketCreator.showChair(player.getId(), 0), false);

        if (player.getFishingTask() != null && !player.getFishingTask().isCanceled()) {
            player.getFishingTask().cancel();
            player.dropMessage(5, "You stopped fishing");
            player.announce(MaplePacketCreator.earnTitleMessage("You stopped fishing"));
        }
        return null;
    }
}

