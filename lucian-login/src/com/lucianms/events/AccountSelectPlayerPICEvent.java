package com.lucianms.events;

import com.lucianms.client.LoginState;
import com.lucianms.nio.receive.MaplePacketReader;
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

        if (getClient().hasBannedMac() || getClient().hasBannedHWID() || !getClient().isPlayerBelonging(playerID)) {
            getClient().getSession().close();
            return null;
        }

        try {
            getClient().setLoginState(LoginState.Transfer);
            String[] socket = getClient().getChannelServer().getIP().split(":");
            getClient().announce(MaplePacketCreator.getServerIP(InetAddress.getByName(socket[0]), Integer.parseInt(socket[1]), playerID));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return null;
    }
}
