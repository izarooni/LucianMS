package com.lucianms.events;

import com.lucianms.client.LoginState;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.channel.MapleChannel;
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
        if (getClient().checkPic(PIC) || !getClient().isPlayerBelonging(playerID)) {
            try {
                getClient().setLoginState(LoginState.Transfer);
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
