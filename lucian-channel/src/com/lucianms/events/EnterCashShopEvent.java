package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleClient;
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
        MapleClient client = getClient();
        MapleCharacter player = client.getPlayer();

        if (player.getCashShop().isOpened()) {
            return null;
        }

        Server.getPlayerBuffStorage().put(player.getId(), player.getAllBuffs());

        player.cancelBuffEffects();

        client.announce(MaplePacketCreator.openCashShop(client, false));

        player.saveToDB();
        player.getCashShop().open(true);

        if (player.getFakePlayer() != null) {
            player.getFakePlayer().setFollowing(false);
            player.getMap().removeFakePlayer(player.getFakePlayer());
        }

        player.getMap().removePlayer(player);
        client.getWorldServer().getPlayerStorage().remove(player.getId());
        client.announce(MaplePacketCreator.showCashInventory(client));
        client.announce(MaplePacketCreator.showGifts(player.getCashShop().loadGifts()));
        client.announce(MaplePacketCreator.showWishList(player, false));
        client.announce(MaplePacketCreator.showCash(player));
        return null;
    }
}
