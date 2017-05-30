package server.events.custom;

import client.MapleCharacter;
import net.server.channel.handlers.UseItemHandler;
import server.life.FakePlayer;
import tools.MaplePacketCreator;
import tools.annotation.PacketWorker;

/**
 * @author izarooni
 */
public class CloneController extends GenericEvent {

    public CloneController() {
        registerAnnotationPacketEvents(this);
    }

    @PacketWorker
    public void onItemUse(UseItemHandler event) {
        MapleCharacter player = event.getClient().getPlayer();
        FakePlayer fPlayer = player.getFakePlayer();
        if (fPlayer == null) {
            return;
        }
        if (event.getItemId() == 2002002) { // toggle follow
            fPlayer.setFollowing(!fPlayer.isFollowing());
            player.dropMessage("Your clone is " + (fPlayer.isFollowing() ? "now" : "no longer") + " following you");
            event.setCanceled(true);
        } else if (event.getItemId() == 2002003) { // unregister
            player.removeGenericEvent(this);
            fPlayer.sendDestroyData(event.getClient());
            player.dropMessage("Unregistered!");
            event.setCanceled(true);
        }
        if (event.isCanceled()) {
            player.announce(MaplePacketCreator.enableActions());
        }
    }
}
