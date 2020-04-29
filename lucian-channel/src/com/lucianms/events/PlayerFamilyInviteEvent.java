package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.constants.ServerConstants;
import com.lucianms.nio.receive.MaplePacketReader;
import tools.MaplePacketCreator;

/**
 * @author Jay Estrella
 * @author izarooni
 */
public class PlayerFamilyInviteEvent extends PacketEvent {

    private String username;

    @Override
    public void processInput(MaplePacketReader reader) {
        username = reader.readMapleAsciiString();
        if (!ServerConstants.GAME.EnableFamilies) {
            setCanceled(true);
        }
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        MapleCharacter target = getClient().getWorldServer().getPlayerStorage().find(p -> p.getName().equalsIgnoreCase(username));
        if (target != null) {
            target.getClient().announce(MaplePacketCreator.sendFamilyInvite(player.getId(), username));
            player.dropMessage("The invite has been sent.");
        } else {
            player.dropMessage("The player cannot be found!");
        }
        getClient().announce(MaplePacketCreator.enableActions());
        return null;
    }
}

