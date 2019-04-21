package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.life.FakePlayer;
import com.lucianms.server.maps.MapleMapObject;
import tools.MaplePacketCreator;

/**
 * @author izarooni
 */
public class ViewCharacterInfoEvent extends PacketEvent {

    private int playerId;

    @Override
    public void processInput(MaplePacketReader reader) {
        reader.skip(4);
        playerId = reader.readInt();
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        MapleMapObject target = player.getMap().getMapObject(playerId);
        if (player.isDebug() && target instanceof FakePlayer) {
            FakePlayer fakePlayer = (FakePlayer) target;
            fakePlayer.setFollowing(!fakePlayer.isFollowing());
            player.sendMessage("{} is {} following", fakePlayer.getName(), (fakePlayer.isFollowing() ? "now" : "no longer"));
        }
        if (target instanceof MapleCharacter) {
            getClient().announce(MaplePacketCreator.charInfo((MapleCharacter) target));
        } else {
            getClient().announce(MaplePacketCreator.enableActions());
        }
        return null;
    }
}
