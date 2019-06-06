package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleClient;
import com.lucianms.client.SpamTracker;
import com.lucianms.nio.receive.MaplePacketReader;

/**
 * @author izarooni
 */
public class KeepAliveEvent extends PacketEvent {

    private static final long SaveInterval = 1000 * 60 * 5;

    @Override
    public boolean inValidState() {
        return true;
    }

    @Override
    public void processInput(MaplePacketReader reader) {
    }

    @Override
    public Object onPacket() {
        MapleClient client = getClient();
        MapleCharacter player = client.getPlayer();
        if (player != null) {
            SpamTracker.SpamData tracker = player.getSpamTracker(SpamTracker.SpamOperation.PlayerSave);
            if (tracker.testFor(SaveInterval)) {
                player.saveToDB();
            }
            tracker.record();

            float latency = System.currentTimeMillis() - client.getKeepAliveRequest();
            if (client.getNetworkLatency() < 0) {
                player.sendMessage("ping took {}ms", latency);
            }
            client.setNetworkLatency(latency);
        }
        return null;
    }
}
