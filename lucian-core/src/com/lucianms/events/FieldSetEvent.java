package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.maps.MapleMap;
import com.lucianms.server.world.MapleParty;
import com.lucianms.server.world.MaplePartyCharacter;

public class FieldSetEvent extends PacketEvent {

    @Override
    public void processInput(MaplePacketReader reader) {
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        MapleMap map = player.getMap();

        player.setRates();
        player.checkBerserk();

        MapleParty party = player.getParty();
        if (party != null) {
            MaplePartyCharacter member = party.get(player.getId());
            if (member != null) {
                member.setFieldID(map.getId());
                player.updatePartyMemberHP();
                player.receivePartyMemberHP();
            } else {
                player.setPartyID(0);
            }
        }
        return null;
    }
}
