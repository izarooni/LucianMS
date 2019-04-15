package com.lucianms.server.handlers.login;

import com.lucianms.client.LoginState;
import com.lucianms.events.PacketEvent;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.channel.MapleChannel;
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
        if (getClient().hasBannedMac() || !getClient().isPlayerBelonging(playerID)) {
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
            getClient().setLoginState(LoginState.Transfer);
            String[] socket = getClient().getChannelServer().getIP().split(":");
            getClient().announce(MaplePacketCreator.getServerIP(InetAddress.getByName(socket[0]), Integer.parseInt(socket[1]), playerID));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return null;
    }
}
