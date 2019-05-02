package com.lucianms.events;

import com.lucianms.BanManager;
import com.lucianms.client.MapleClient;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.channel.MapleChannel;

/**
 * @author izarooni
 */
public class AccountPlayerSelectEvent extends UserTransferEvent {

    private int playerID;
    private String macs;

    @Override
    public void processInput(MaplePacketReader reader) {
        playerID = reader.readInt();
        macs = reader.readMapleAsciiString();
    }

    @Override
    public Object onPacket() {
        MapleClient client = getClient();
        if (BanManager.isBanned(client) || !client.isPlayerBelonging(playerID)) {
            client.getSession().close();
            return null;
        }

        MapleChannel cserv = client.getChannelServer();
        client.updateMacs(macs);
        issueConnect(cserv.getNetworkAddress(), cserv.getPort(), playerID);
        return null;
    }
}