package net.server.handlers.login;

import client.MapleClient;
import constants.ServerConstants;
import net.AbstractMaplePacketHandler;
import net.server.Server;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class CharSelectedWithPicHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient client) {

        String pic = slea.readMapleAsciiString();
        int playerId = slea.readInt();
        String macs = slea.readMapleAsciiString();
        String hwid = slea.readMapleAsciiString();
        client.updateMacs(macs);
        client.updateHWID(hwid);

        if (client.hasBannedMac() || client.hasBannedHWID() || !client.playerBelongs(playerId)) {
            client.getSession().close(true);
            return;
        }
//        if (ServerConstants.ENABLE_PIC && !client.checkPic(pic)) {
//            client.announce(MaplePacketCreator.wrongPic());
//            return;
//        }
        if (client.getIdleTask() != null) {
            client.getIdleTask().cancel(true);
        }
        client.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION);

        try {
            String[] socket = Server.getInstance().getIP(client.getWorld(), client.getChannel()).split(":");
            client.announce(MaplePacketCreator.getServerIP(InetAddress.getByName(socket[0]), Integer.parseInt(socket[1]), playerId));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }
}
