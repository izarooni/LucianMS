package com.lucianms.server.events.login;

import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.events.PacketEvent;
import constants.ServerConstants;
import net.server.Server;
import net.server.world.MapleWorld;
import tools.MaplePacketCreator;

/**
 * @author izarooni
 */
public class WorldListEvent extends PacketEvent {

    @Override
    public void processInput(MaplePacketReader reader) {
    }

    @Override
    public Object onPacket() {
        Server server = Server.getInstance();
        for (MapleWorld world : server.getWorlds()) {
            getClient().announce(MaplePacketCreator.getServerList(world.getId(), ServerConstants.WORLD_NAMES[world.getId()], world.getFlag(), world.getEventMessage(), world.getChannels()));
        }
        getClient().announce(MaplePacketCreator.getEndOfServerList());
        getClient().announce(MaplePacketCreator.selectWorld(0));//too lazy to make a check lol
        getClient().announce(MaplePacketCreator.sendRecommended(server.worldRecommendedList()));
        return null;
    }
}