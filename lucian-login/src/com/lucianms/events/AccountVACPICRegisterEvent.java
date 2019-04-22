package com.lucianms.events;

import com.lucianms.client.MapleClient;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.channel.MapleChannel;
import tools.Randomizer;

import java.util.List;

/**
 * @author izarooni
 */
public class AccountVACPICRegisterEvent extends UserTransferEvent {

    private String macs;
    private String PIC;
    private int playerID;
    private int worldID;

    @Override
    public void processInput(MaplePacketReader reader) {
        reader.readByte();
        playerID = reader.readInt();
        worldID = reader.readInt();
        macs = reader.readMapleAsciiString();
        reader.readMapleAsciiString();
        PIC = reader.readMapleAsciiString();

        checkLoginAvailability();
    }

    @Override
    public Object onPacket() {
        MapleClient client = getClient();
        MapleChannel cserv = client.getChannelServer();
        if (client.hasBannedMac() || !client.isPlayerBelonging(playerID)) {
            client.getSession().close();
            return null;
        }
        client.setWorld(worldID);
        List<MapleChannel> channels = client.getWorldServer().getChannels();
        client.setChannel(Randomizer.nextInt(channels.size()) + 1);
        client.updateMacs(macs);
        client.setPic(PIC);
        issueConnect(cserv.getNetworkAddress(), cserv.getPort(), playerID);
        return null;
    }
}
