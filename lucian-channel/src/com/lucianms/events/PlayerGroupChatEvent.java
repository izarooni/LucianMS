package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.command.CommandWorker;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.Server;
import com.lucianms.server.world.MapleWorld;
import tools.MaplePacketCreator;

/**
 * @author izarooni
 */
public class PlayerGroupChatEvent extends PacketEvent {

    private String content;
    private byte action;
    private int[] recipients;

    @Override
    public void processInput(MaplePacketReader reader) {
        action = reader.readByte();
        byte count = reader.readByte();
        recipients = new int[count];
        for (int i = 0; i < count; i++) {
            recipients[i] = reader.readInt();
        }
        content = reader.readMapleAsciiString();
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        if (CommandWorker.isCommand(content)) {
            if (CommandWorker.process(getClient(), content, false)) {
                return null;
            }
        }
        MapleWorld world = getClient().getWorldServer();
        if (action == 0) {
            world.buddyChat(recipients, player.getId(), player.getName(), content);
        } else if (action == 1 && player.getParty() != null) {
            player.getParty().sendPacketExclude(MaplePacketCreator.multiChat(player.getName(), content, 1), player);
        } else if (action == 2 && player.getGuildId() > 0) {
            Server.guildChat(player.getGuildId(), player.getName(), player.getId(), content);
        } else if (action == 3 && player.getGuild() != null) {
            int allianceId = player.getGuild().getAllianceId();
            if (allianceId > 0) {
                Server.allianceMessage(allianceId, MaplePacketCreator.multiChat(player.getName(), content, 3), player.getId(), -1);
            }
        }
        return null;
    }
}
