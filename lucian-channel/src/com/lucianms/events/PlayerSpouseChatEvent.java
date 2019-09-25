package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.Relationship;
import com.lucianms.command.CommandWorker;
import com.lucianms.nio.SendOpcode;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.nio.send.MaplePacketWriter;
import tools.MaplePacketCreator;

/**
 * @author izarooni
 */
public class PlayerSpouseChatEvent extends PacketEvent {

    public static byte[] getMessageFailed() {
        MaplePacketWriter w = new MaplePacketWriter(2);
        w.writeShort(SendOpcode.SPOUSE_CHAT.getValue());
        w.write(4);
        w.write(0);
        return w.getPacket();
    }

    private String username;
    private String content;

    @Override
    public void processInput(MaplePacketReader reader) {
        username = reader.readMapleAsciiString();
        content = reader.readMapleAsciiString();
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        Relationship rltn = player.getRelationship();
        if (CommandWorker.isCommand(content)) {
            if (CommandWorker.process(getClient(), content, false)) {
                return null;
            }
        }
        if (rltn.getStatus() == Relationship.Status.Married) {
            MapleCharacter target = getClient().getWorldServer().findPlayer(p -> p.getName().equalsIgnoreCase(username));
            if (target != null && target.getId() == player.getRelationship().getPartnerID(player)) {
                getClient().announce(MaplePacketCreator.sendSpouseChat(target, content, false));
                target.announce(MaplePacketCreator.sendSpouseChat(player, content, true));
            } else {
                getClient().announce(getMessageFailed());
            }
        } else {
            getClient().announce(getMessageFailed());
        }
        return null;
    }
}
