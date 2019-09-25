package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleClient;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.life.FakePlayer;
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
        MapleClient client = getClient();
        MapleCharacter player = client.getPlayer();
        MapleCharacter target = player.getMap().getCharacterById(playerId);

        if (player.isDebug() && target instanceof FakePlayer) {
            FakePlayer fakePlayer = (FakePlayer) target;
            fakePlayer.setFollowing(!fakePlayer.isFollowing());
            player.sendMessage("{} is {} following", fakePlayer.getName(), (fakePlayer.isFollowing() ? "now" : "no longer"));
        } else if (target != null) {
            client.announce(MaplePacketCreator.getCharacterInfo(target));
        } else {
            client.announce(MaplePacketCreator.enableActions());
        }
        return null;
    }
}
