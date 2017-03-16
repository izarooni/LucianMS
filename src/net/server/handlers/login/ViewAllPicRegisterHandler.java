package net.server.handlers.login;

import client.MapleClient;
import java.net.InetAddress;
import java.net.UnknownHostException;
import net.AbstractMaplePacketHandler;
import net.server.Server;
import tools.MaplePacketCreator;
import tools.Randomizer;
import tools.data.input.SeekableLittleEndianAccessor;

public final class ViewAllPicRegisterHandler extends AbstractMaplePacketHandler { //Gey class name lol


    @Override
    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient client) {
        slea.readByte();
        int playerId = slea.readInt();
        client.setWorld(slea.readInt()); //world
        int channel = Randomizer.rand(0, Server.getInstance().getWorld(client.getWorld()).getChannels().size());
        client.setChannel(channel);
        String mac = slea.readMapleAsciiString();
        client.updateMacs(mac);
        if (client.hasBannedMac() || !client.playerBelongs(playerId)) {
            client.getSession().close(true);
            return;
        }
        slea.readMapleAsciiString();
        String pic = slea.readMapleAsciiString();
        client.setPic(pic);
        if (client.getIdleTask() != null) {
            client.getIdleTask().cancel(true);
        }
        client.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION);
        String[] socket = Server.getInstance().getIP(client.getWorld(), channel).split(":");
        try {
            client.announce(MaplePacketCreator.getServerIP(InetAddress.getByName(socket[0]), Integer.parseInt(socket[1]), playerId));
        } catch (UnknownHostException e) {
        }
    }
}
