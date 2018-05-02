package com.lucianms.server.events.channel;

import client.MapleCharacter;
import net.PacketEvent;
import server.MaplePortal;
import server.MapleTrade;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 * @author izarooni
 */
public final class ChangeMapSpecialEvent extends PacketEvent {

    private String startwp;

    @Override
    public void process(SeekableLittleEndianAccessor slea) {
        slea.skip(1);
        startwp = slea.readMapleAsciiString();
        slea.skip(2);
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        MaplePortal portal = player.getMap().getPortal(startwp);

        if (portal != null) {
            if (player.isGM() && player.isDebug()) {
                player.sendMessage("[DEBUG] ID: {}, Name: {}/{}, Target map: {}, Location: x:{}/y:{}", portal.getId(), portal.getName(), portal.getScriptName(), portal.getTarget(), portal.getPosition().x, portal.getPosition().y);
                player.announce(MaplePacketCreator.enableActions());
                return null;
            }

            if (player.isBanned() || player.portalDelay() > System.currentTimeMillis() || player.getBlockedPortals().contains(portal.getScriptName())) {
                player.announce(MaplePacketCreator.enableActions());
                return null;
            }
            if (player.getTrade() != null) {
                MapleTrade.cancelTrade(player);
            }
            portal.enterPortal(getClient());
        }
        return null;
    }
}
