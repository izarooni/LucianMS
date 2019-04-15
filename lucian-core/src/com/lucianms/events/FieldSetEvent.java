package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.maps.MapleMap;
import com.lucianms.server.world.MapleParty;
import com.lucianms.server.world.PartyOperation;
import tools.MaplePacketCreator;

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
            party.get(player.getId()).setFieldID(map.getId());
            player.announce(MaplePacketCreator.updateParty(getClient().getChannel(), party, PartyOperation.SILENT_UPDATE, null));
            player.updatePartyMemberHP();
        }
        return null;
    }
}
