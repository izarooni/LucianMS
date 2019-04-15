package com.lucianms.events;

import com.lucianms.client.LoginState;
import com.lucianms.nio.receive.MaplePacketReader;
import tools.MaplePacketCreator;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author izarooni
 */
public class AccountPlayerSelectEvent extends PacketEvent {

    private int playerID;
    private String macs;

    @Override
    public void processInput(MaplePacketReader reader) {
        playerID = reader.readInt();
        macs = reader.readMapleAsciiString();

        if (getClient().hasBannedMac() || !getClient().isPlayerBelonging(playerID)) {
            getClient().getSession().close();
            setCanceled(true);
        }
    }

    @Override
    public Object onPacket() {
        getClient().updateMacs(macs);
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