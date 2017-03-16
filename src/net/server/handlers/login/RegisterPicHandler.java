package net.server.handlers.login;

import client.MapleClient;
import net.AbstractMaplePacketHandler;
import net.server.Server;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

import java.net.InetAddress;
import java.net.UnknownHostException;

public final class RegisterPicHandler extends AbstractMaplePacketHandler {

    @Override
    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient client) {
        slea.readByte();
        int playerId = slea.readInt();
        String macs = slea.readMapleAsciiString();
        String hwid = slea.readMapleAsciiString();

        client.updateMacs(macs);
        client.updateHWID(hwid);

        if (client.hasBannedMac() || client.hasBannedHWID() || !client.playerBelongs(playerId)) {
            client.getSession().close(true);
            return;
        }

        String pic = slea.readMapleAsciiString();
        if (client.getPic() == null || client.getPic().equals("")) {
            client.setPic(pic);
            if (client.getIdleTask() != null) {
                client.getIdleTask().cancel(true);
            }
            try {
                client.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION);
                String[] socket = Server.getInstance().getIP(client.getWorld(), client.getChannel()).split(":");
                client.announce(MaplePacketCreator.getServerIP(InetAddress.getByName(socket[0]), Integer.parseInt(socket[1]), playerId));
            } catch (UnknownHostException ignore) {
            }
        } else {
            client.getSession().close(true);
        }
    }
}