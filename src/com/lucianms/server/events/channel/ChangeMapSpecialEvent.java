package com.lucianms.server.events.channel;

import client.MapleCharacter;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.events.PacketEvent;
import server.MaplePortal;
import server.MapleTrade;
import tools.MaplePacketCreator;

/**
 * @author izarooni
 */
public final class ChangeMapSpecialEvent extends PacketEvent {

    private String startwp;

    @Override
    public void processInput(MaplePacketReader reader) {
        reader.skip(1);
        startwp = reader.readMapleAsciiString();
        reader.skip(2);
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
