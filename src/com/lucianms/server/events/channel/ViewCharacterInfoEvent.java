package com.lucianms.server.events.channel;

import client.MapleCharacter;
import net.PacketEvent;
import server.maps.MapleMapObject;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 * @author izarooni
 */
public class ViewCharacterInfoEvent extends PacketEvent {

    private int playerId;

    @Override
    public void process(SeekableLittleEndianAccessor slea) {
        slea.skip(4);
        playerId = slea.readInt();
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        MapleMapObject target = player.getMap().getMapObject(playerId);
        if (target != null && target instanceof MapleCharacter) {
            getClient().announce(MaplePacketCreator.charInfo((MapleCharacter) target));
        } else {
            getClient().announce(MaplePacketCreator.enableActions());
        }
        return null;
    }
}
