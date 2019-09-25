package com.lucianms.events;

import com.lucianms.client.LoginState;
import com.lucianms.server.Server;
import tools.MaplePacketCreator;

import java.net.InetAddress;

/**
 * @author izarooni
 */
abstract class UserTransferEvent extends PacketEvent {

    void checkLoginAvailability() {
        if (!Server.getToggles().checkProperty("server_online", false)) {
            getClient().announce(MaplePacketCreator.getSelectPlayerFailed((byte) 15, (byte) 0));
            getClient().announce(MaplePacketCreator.serverNotice(0,
                    "The server is currently under maintenance." +
                            "\r\nFeel free to keep your game open and" +
                            "\r\ntry again in a few minutes." +
                            "\r\nYou will otherwise be noticed" +
                            "\r\nwhen you may login."));
            setCanceled(true);
        }
    }

    void issueConnect(InetAddress address, int port, int playerID) {
        getClient().updateLoginState(LoginState.Transfer);
        getClient().announce(MaplePacketCreator.getServerIP(address, port, playerID));
    }
}
