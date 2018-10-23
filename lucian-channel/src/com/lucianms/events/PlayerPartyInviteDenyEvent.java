package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.events.PacketEvent;
import tools.MaplePacketCreator;

/**
 * @author izarooni
 */
public class PlayerPartyInviteDenyEvent extends PacketEvent {

    private String username;

    @Override
    public void processInput(MaplePacketReader reader) {
        reader.readByte(); // result
        username = reader.readMapleAsciiString();
    }

    @Override
    public Object onPacket() {
        MapleCharacter sourcePlayer = getClient().getChannelServer().getPlayerStorage().getPlayerByName(username);
        if (sourcePlayer != null) {
            sourcePlayer.getClient().announce(MaplePacketCreator.partyStatusMessage(23, getClient().getPlayer().getName()));
        }
        return null;
    }
}