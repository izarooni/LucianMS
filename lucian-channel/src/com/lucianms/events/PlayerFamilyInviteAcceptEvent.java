package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.constants.ServerConstants;
import com.lucianms.nio.receive.MaplePacketReader;
import tools.MaplePacketCreator;

/**
 * @author Jay Estrella
 * @author izarooni
 */
public class PlayerFamilyInviteAcceptEvent extends PacketEvent {

    private int playerID;

    @Override
    public void processInput(MaplePacketReader reader) {
        playerID = reader.readInt();
    }

    @Override
    public Object onPacket() {
        if (!ServerConstants.USE_FAMILY_SYSTEM) {
            return null;
        }
        MapleCharacter inviter = getClient().getWorldServer().getPlayerStorage().getPlayerByID(playerID);
        if (inviter != null) {
            inviter.getClient().announce(MaplePacketCreator.sendFamilyJoinResponse(true, getClient().getPlayer().getName()));
        }
        getClient().announce(MaplePacketCreator.sendFamilyMessage(0, 0));
        return null;
    }
}
