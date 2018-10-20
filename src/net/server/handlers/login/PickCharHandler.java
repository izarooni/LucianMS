package net.server.handlers.login;

import client.MapleClient;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.events.PacketEvent;
import net.server.channel.MapleChannel;
import tools.MaplePacketCreator;
import tools.Randomizer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

/**
 * @author izarooni
 */
public class PickCharHandler extends PacketEvent {

    private int playerID;
    private int worldID;
    private String macs;

    @Override
    public void processInput(MaplePacketReader reader) {
        playerID = reader.readInt();
        worldID = reader.readInt();
        macs = reader.readMapleAsciiString();
        if (getClient().hasBannedMac() || !getClient().playerBelongs(playerID)) {
            getClient().getSession().close();
            setCanceled(true);
        }
    }

    @Override
    public Object onPacket() {
        getClient().setWorld(worldID);
        getClient().updateMacs(macs);
        List<MapleChannel> channels = getClient().getWorldServer().getChannels();
        getClient().setChannel(Randomizer.nextInt(channels.size()) + 1);

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
