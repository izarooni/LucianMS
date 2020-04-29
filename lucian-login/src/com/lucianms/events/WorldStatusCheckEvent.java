package com.lucianms.events;

import com.lucianms.constants.ServerConstants;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.Server;
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
        int userCount = Server.getWorld(world).getPlayerStorage().size();
        if (userCount >= ServerConstants.LOGIN.ChannelUserCapacity) {
            status = 2;
        } else if (userCount >= ServerConstants.LOGIN.ChannelUserCapacity * 0.8) {
            status = 1;
        } else {
            status = 0;
        }
        getClient().announce(MaplePacketCreator.getServerStatus(status));
        return null;
    }
}
