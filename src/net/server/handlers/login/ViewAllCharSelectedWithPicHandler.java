package net.server.handlers.login;

import client.MapleClient;
import net.AbstractMaplePacketHandler;
import net.server.Server;
import tools.MaplePacketCreator;
import tools.Randomizer;
import tools.data.input.SeekableLittleEndianAccessor;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ViewAllCharSelectedWithPicHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient client) {

        String pic = slea.readMapleAsciiString();
        int playerId = slea.readInt();
        int world = slea.readInt();//world
        client.setWorld(world);
        int channel = Randomizer.rand(0, Server.getInstance().getWorld(world).getChannels().size());
        client.setChannel(channel);
        String macs = slea.readMapleAsciiString();
        client.updateMacs(macs);

        if (client.hasBannedMac()) {
            client.getSession().close(true);
            return;
        }
        if (client.checkPic(pic) || !client.playerBelongs(playerId)) {
            if (client.getIdleTask() != null) {
                client.getIdleTask().cancel(true);
            }
            try {
                client.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION);
                String[] socket = Server.getInstance().getIP(client.getWorld(), client.getChannel()).split(":");
                client.announce(MaplePacketCreator.getServerIP(InetAddress.getByName(socket[0]), Integer.parseInt(socket[1]), playerId));
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        } else {
            client.announce(MaplePacketCreator.wrongPic());
        }
    }
}
