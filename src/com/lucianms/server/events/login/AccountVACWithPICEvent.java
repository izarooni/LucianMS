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

public class AccountVACWithPICEvent extends PacketEvent {

    private String PIC;
    private String macs;
    private int playerID;
    private int worldID;

    @Override
    public void processInput(MaplePacketReader reader) {
        PIC = reader.readMapleAsciiString();
        playerID = reader.readInt();
        worldID = reader.readInt();
        macs = reader.readMapleAsciiString();
    }

    @Override
    public Object onPacket() {
        getClient().setWorld(worldID);
        List<MapleChannel> channels = getClient().getWorldServer().getChannels();
        getClient().setChannel(Randomizer.nextInt(channels.size()) + 1);
        getClient().updateMacs(macs);

        if (getClient().hasBannedMac()) {
            getClient().getSession().close();
            return null;
        }
        if (getClient().checkPic(PIC) || !getClient().playerBelongs(playerID)) {
            try {
                getClient().updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION);
                String[] socket = getClient().getChannelServer().getIP().split(":");
                getClient().announce(MaplePacketCreator.getServerIP(InetAddress.getByName(socket[0]), Integer.parseInt(socket[1]), playerID));
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        } else {
            getClient().announce(MaplePacketCreator.wrongPic());
        }
        return null;
    }
}
