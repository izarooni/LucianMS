package com.lucianms.events;

import com.lucianms.client.MapleClient;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.channel.MapleChannel;

/**
 * @author izarooni
 */
public class AccountRegisterPICEvent extends UserTransferEvent {

    private String macs;
    private String hwid;
    private String PIC;
    private int playerID;

    @Override
    public void processInput(MaplePacketReader reader) {
        reader.readByte();
        playerID = reader.readInt();
        macs = reader.readMapleAsciiString();
        hwid = reader.readMapleAsciiString();
        PIC = reader.readMapleAsciiString();

        checkLoginAvailability();
    }

    @Override
    public Object onPacket() {
        MapleClient client = getClient();
        MapleChannel cserv = client.getChannelServer();
        client.updateMacs(macs);
        client.updateHWID(hwid);

        if (client.getPic() != null || client.hasBannedMac() || client.hasBannedHWID() || !client.isPlayerBelonging(playerID)) {
            client.getSession().close();
            return null;
        }
        client.setPic(PIC);
        issueConnect(cserv.getNetworkAddress(), cserv.getPort(), playerID);
        return null;
    }
}