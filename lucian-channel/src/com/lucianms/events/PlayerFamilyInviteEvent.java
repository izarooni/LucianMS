package com.lucianms.events;

import com.lucianms.constants.ServerConstants;
import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleClient;
import com.lucianms.events.PacketEvent;
import com.lucianms.nio.receive.MaplePacketReader;
import tools.MaplePacketCreator;
import tools.data.input.LittleEndianReader;

/**
 * @author Jay Estrella
 * @author izarooni
 */
public class PlayerFamilyInviteEvent extends PacketEvent {

    private String username;

    @Override
    public void processInput(MaplePacketReader reader) {
        username = reader.readMapleAsciiString();
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        if (!ServerConstants.USE_FAMILY_SYSTEM){
    		return null;
    	}
        MapleCharacter target = getClient().getChannelServer().getPlayerStorage().getPlayerByName(username);
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
