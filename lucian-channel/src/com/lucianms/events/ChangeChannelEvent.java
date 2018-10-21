package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.SpamTracker;
import com.lucianms.nio.receive.MaplePacketReader;
import tools.MaplePacketCreator;

/**
 * @author Matze
 * @author izarooni
 */
public class ChangeChannelEvent extends PacketEvent {

    private int channelID;

    @Override
    public void processInput(MaplePacketReader reader) {
        MapleCharacter player = getClient().getPlayer();
        channelID = reader.readByte() + 1;
        if (getClient().getChannel() == channelID) {
            setCanceled(true);
        }
        if (player.getCashShop().isOpened()
                || player.getMiniGame() != null
                || player.getMapId() == 333
                || player.getPlayerShop() != null) {
            setCanceled(true);
        }
        SpamTracker.SpamData spamTracker = player.getSpamTracker(SpamTracker.SpamOperation.ChangeChannel);
        if (spamTracker.testFor(1000)) {
            getClient().announce(MaplePacketCreator.enableActions());
            setCanceled(true);
        } else if (player.getMapId() == 98) {
            player.dropMessage(1, "You can't do it here in this map.");
            getClient().announce(MaplePacketCreator.enableActions());
            setCanceled(true);
        }
        spamTracker.record();
    }

    @Override
    public Object onPacket() {
        getClient().changeChannel(channelID);
        return null;
    }
}