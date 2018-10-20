package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.autoban.Cheater;
import com.lucianms.client.autoban.Cheats;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.events.PacketEvent;
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
        Cheater.CheatEntry entry = player.getCheater().getCheatEntry(Cheats.FastChannelChange);
        if (System.currentTimeMillis() - entry.latestOperationTimestamp < 1000) {
            getClient().announce(MaplePacketCreator.enableActions());
            setCanceled(true);
        } else if (player.getMapId() == 98) { // outer space map
            player.dropMessage(1, "You can't do it here in this map.");
            getClient().announce(MaplePacketCreator.enableActions());
            setCanceled(true);
        }
    }

    @Override
    public Object onPacket() {
        Cheater.CheatEntry entry = getClient().getPlayer().getCheater().getCheatEntry(Cheats.FastChannelChange);
        entry.latestOperationTimestamp = System.currentTimeMillis();
        getClient().changeChannel(channelID);
        return null;
    }
}