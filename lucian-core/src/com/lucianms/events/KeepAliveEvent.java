package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleClient;
import com.lucianms.nio.receive.MaplePacketReader;

/**
 * @author izarooni
 */
public class KeepAliveEvent extends PacketEvent {

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

        float latency = System.currentTimeMillis() - client.getKeepAliveRequest();

        if (client.getNetworkLatency() < 0) {
            player.sendMessage("ping took {}ms", latency);
        }
        client.setNetworkLatency(latency);
        return null;
    }
}
