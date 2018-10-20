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
public class AccountPlayerSelectEvent extends PacketEvent {

    private int playerID;
    private String macs;

    @Override
    public void processInput(MaplePacketReader reader) {
        playerID = reader.readInt();
        macs = reader.readMapleAsciiString();

        if (getClient().hasBannedMac() || !getClient().playerBelongs(playerID)) {
            getClient().getSession().close();
            setCanceled(true);
        }
    }

    @Override
    public Object onPacket() {
        getClient().updateMacs(macs);
        try {
            getClient().updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION);
            String[] socket = getClient().getChannelServer().getIP().split(":");
            getClient().announce(MaplePacketCreator.getServerIP(InetAddress.getByName(socket[0]), Integer.parseInt(socket[1]), playerID));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return null;
    }
}