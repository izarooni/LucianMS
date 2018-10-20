package net.server.channel.handlers;

import client.MapleCharacter;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.events.PacketEvent;
import tools.MaplePacketCreator;

/**
 * @author Xterminator
 * @author izarooni
 */
public class PlayerGuildInviteDenyEvent extends PacketEvent {

    private String username;

    @Override
    public void processInput(MaplePacketReader reader) {
        reader.readByte();
        username = reader.readMapleAsciiString();
    }

    @Override
    public Object onPacket() {
        MapleCharacter sourcePlayer = getClient().getChannelServer().getPlayerStorage().getPlayerByName(username);
        if (sourcePlayer != null) {
            sourcePlayer.getClient().announce(MaplePacketCreator.denyGuildInvitation(getClient().getPlayer().getName()));
        }
        return null;
    }
}
