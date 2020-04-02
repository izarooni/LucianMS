package com.lucianms.events;

import com.lucianms.BanManager;
import com.lucianms.client.MapleClient;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.channel.MapleChannel;
import tools.MaplePacketCreator;

/**
 * @author izarooni
 */
public class SPWSelectPlayerEvent extends UserTransferEvent {

    private String PIC;
    private String macs;
    private String hwid;
    private int playerID;

    @Override
    public void processInput(MaplePacketReader reader) {
        PIC = reader.readMapleAsciiString();
        playerID = reader.readInt();
        macs = reader.readMapleAsciiString();
        hwid = reader.readMapleAsciiString();

        checkLoginAvailability();
    }

    @Override
    public Object onPacket() {
        MapleClient client = getClient();
        MapleChannel cserv = client.getChannelServer();

        if(client.checkPic(PIC)) {
            if (BanManager.isBanned(client) || !client.isPlayerBelonging(playerID)) {
                client.getSession().close();

            }

            client.updateMacs(macs);
            client.updateHWID(hwid);
            issueConnect(cserv.getNetworkAddress(), cserv.getPort(), playerID);

        } else {
            client.announce(MaplePacketCreator.wrongPic());

        }
        return null;
    }
}
