
package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleClient;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.Server;
import com.lucianms.server.channel.MapleChannel;
import com.lucianms.server.maps.HiredMerchant;
import tools.MaplePacketCreator;

/**
 * @author kevintjuh93 :3
 * @author izarooni
 */
public class PlayerRemoteStoreEvent extends PacketEvent {

    @Override
    public void processInput(MaplePacketReader reader) {

    }

    @Override
    public Object onPacket() {
        MapleCharacter chr = getClient().getPlayer();
        HiredMerchant hm = getMerchant(getClient());
        if (chr.hasMerchant() && hm != null) {
            if (hm.getChannel() == chr.getClient().getChannel()) {
                hm.setOpen(false);
                hm.removeAllVisitors("");
                chr.setHiredMerchant(hm);
                chr.announce(MaplePacketCreator.getHiredMerchant(chr, hm, false));
            } else {
                getClient().announce(MaplePacketCreator.remoteChannelChange((byte) (hm.getChannel() - 1)));
            }
            return null;
        } else {
            chr.dropMessage(1, "You don't have a Merchant open");
        }
        getClient().announce(MaplePacketCreator.enableActions());
        return null;
    }

    private static HiredMerchant getMerchant(MapleClient c) {
        if (c.getPlayer().hasMerchant()) {
            for (MapleChannel cserv : Server.getChannelsFromWorld(c.getWorld())) {
                if (cserv.getHiredMerchants().get(c.getPlayer().getId()) != null) {
                    return cserv.getHiredMerchants().get(c.getPlayer().getId());
                }
            }
        }
        return null;
    }
}
