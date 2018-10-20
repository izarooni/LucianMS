package com.lucianms.server.events.login;

import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.events.PacketEvent;
import constants.ServerConstants;
import net.server.Server;
import net.server.channel.MapleChannel;
import tools.MaplePacketCreator;

/**
 * @author izarooni
 */
public class WorldStatusCheckEvent extends PacketEvent {

    private short world;

    @Override
    public void processInput(MaplePacketReader reader) {
        world = reader.readShort();
    }

    @Override
    public Object onPacket() {
        int status;
        int num = 0;
        for (MapleChannel ch : Server.getInstance().getWorld(world).getChannels()) {
            num += ch.getConnectedClients();
        }
        if (num >= ServerConstants.CHANNEL_LOAD) {
            status = 2;
        } else if (num >= ServerConstants.CHANNEL_LOAD * 0.8) {
            status = 1;
        } else {
            status = 0;
        }
        getClient().announce(MaplePacketCreator.getServerStatus(status));
        return null;
    }
}
