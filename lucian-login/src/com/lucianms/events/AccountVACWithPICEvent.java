package com.lucianms.events;

import com.lucianms.client.MapleClient;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.channel.MapleChannel;
import tools.MaplePacketCreator;
import tools.Randomizer;

import java.util.List;

public class AccountVACWithPICEvent extends UserTransferEvent {

    private String PIC;
    private String macs;
    private int playerID;
    private int worldID;

    @Override
    public void processInput(MaplePacketReader reader) {
        PIC = reader.readMapleAsciiString();
        playerID = reader.readInt();
        worldID = reader.readInt();
        macs = reader.readMapleAsciiString();

        checkLoginAvailability();
    }

    @Override
    public Object onPacket() {
        MapleClient client = getClient();
        MapleChannel cserv = client.getChannelServer();
        List<MapleChannel> channels = client.getWorldServer().getChannels();

        client.setWorld(worldID);
        client.setChannel(Randomizer.nextInt(channels.size()) + 1);
        client.updateMacs(macs);

        if (client.hasBannedMac()) {
            client.getSession().close();
            return null;
        }
        if (!client.checkPic(PIC) || !client.isPlayerBelonging(playerID)) {
            client.announce(MaplePacketCreator.wrongPic());
            return null;
        }
        issueConnect(cserv.getNetworkAddress(), cserv.getPort(), playerID);
        return null;
    }
}
