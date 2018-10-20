package com.lucianms.server.events.login;

import client.MapleClient;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.events.PacketEvent;
import tools.MaplePacketCreator;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author izarooni
 */
public class AccountSelectPlayerPICEvent extends PacketEvent {

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
    }

    @Override
    public Object onPacket() {
        getClient().updateMacs(macs);
        getClient().updateHWID(hwid);

        if (getClient().hasBannedMac() || getClient().hasBannedHWID() || !getClient().playerBelongs(playerID)) {
            getClient().getSession().close();
            return null;
        }
//        if (ServerConstants.ENABLE_PIC && !client.checkPic(pic)) {
//            client.announce(MaplePacketCreator.wrongPic());
//            return;
//        }

        getClient().updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION);

        try {
            String[] socket = getClient().getChannelServer().getIP().split(":");
            getClient().announce(MaplePacketCreator.getServerIP(InetAddress.getByName(socket[0]), Integer.parseInt(socket[1]), playerID));
        } catch (UnknownHostException ignore) {
        }
        return null;
    }
}
