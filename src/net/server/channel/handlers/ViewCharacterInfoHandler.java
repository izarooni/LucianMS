package net.server.channel.handlers;

import client.MapleCharacter;
import net.PacketHandler;
import server.maps.MapleMapObject;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 * @author izarooni
 */
public class ViewCharacterInfoHandler extends PacketHandler {

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
