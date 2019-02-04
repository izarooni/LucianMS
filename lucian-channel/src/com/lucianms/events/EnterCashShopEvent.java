package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.Server;
import tools.MaplePacketCreator;

/**
 * @author Flav
 * @author izarooni
 */
public class EnterCashShopEvent extends PacketEvent {

    @Override
    public void processInput(MaplePacketReader reader) {
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();

        if (player.getCashShop().isOpened()) {
            return null;
        }

        Server.getPlayerBuffStorage().put(player.getId(), player.getAllBuffs());

        player.cancelBuffEffects();
        player.cancelExpirationTask();

        getClient().announce(MaplePacketCreator.openCashShop(getClient(), false));

        player.saveToDB();
        player.getCashShop().open(true);

        if (player.getFakePlayer() != null) {
            player.getFakePlayer().setFollowing(false);
            player.getMap().removeFakePlayer(player.getFakePlayer());
        }

        player.getMap().removePlayer(player);
        getClient().getChannelServer().removePlayer(player);
        getClient().announce(MaplePacketCreator.showCashInventory(getClient()));
        getClient().announce(MaplePacketCreator.showGifts(player.getCashShop().loadGifts()));
        getClient().announce(MaplePacketCreator.showWishList(player, false));
        getClient().announce(MaplePacketCreator.showCash(player));
        return null;
    }
}
