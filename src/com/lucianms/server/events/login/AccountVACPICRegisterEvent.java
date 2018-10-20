package com.lucianms.server.events.login;

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
public class AccountVACPICRegisterEvent extends PacketEvent {

    private String macs;
    private String PIC;
    private int playerID;
    private int worldID;

    @Override
    public void processInput(MaplePacketReader reader) {
        reader.readByte();
        playerID = reader.readInt();
        worldID = reader.readInt();
        macs = reader.readMapleAsciiString();
        reader.readMapleAsciiString();
        PIC = reader.readMapleAsciiString();
    }

    @Override
    public Object onPacket() {
        if (getClient().hasBannedMac() || !getClient().playerBelongs(playerID)) {
            getClient().getSession().close();
            return null;
        }
        getClient().setWorld(worldID);
        List<MapleChannel> channels = getClient().getWorldServer().getChannels();
        getClient().setChannel(Randomizer.nextInt(channels.size()) + 1);
        getClient().updateMacs(macs);
        getClient().setPic(PIC);
        try {
            getClient().updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION);
            String[] socket = getClient().getChannelServer().getIP().split(":");
            getClient().announce(MaplePacketCreator.getServerIP(InetAddress.getByName(socket[0]), Integer.parseInt(socket[1]), playerID));
        } catch (UnknownHostException ignore) {
        }
        return null;
    }
}
