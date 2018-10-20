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
public class AccountRegisterPICEvent extends PacketEvent {

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
    }

    @Override
    public Object onPacket() {
        getClient().updateMacs(macs);
        getClient().updateHWID(hwid);

        if (getClient().hasBannedMac() || getClient().hasBannedHWID() || !getClient().playerBelongs(playerID)) {
            getClient().getSession().close();
            return null;
        }

        if (getClient().getPic() == null || getClient().getPic().isEmpty()) {
            getClient().setPic(PIC);
            try {
                getClient().updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION);
                String[] socket = getClient().getChannelServer().getIP().split(":");
                getClient().announce(MaplePacketCreator.getServerIP(InetAddress.getByName(socket[0]), Integer.parseInt(socket[1]), playerID));
            } catch (UnknownHostException ignore) {
            }
        } else {
            getClient().getSession().close();
        }
        return null;
    }
}