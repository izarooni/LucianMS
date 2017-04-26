package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleClient;
import client.Relationship;
import client.command.CommandWorker;
import net.AbstractMaplePacketHandler;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 * @author izarooni
 */
public class SpouseChatHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient client) {
        MapleCharacter player = client.getPlayer();
        Relationship rltn = player.getRelationship();
        String tUsername = slea.readMapleAsciiString();
        String message = slea.readMapleAsciiString();
        if (CommandWorker.isCommand(message)) {
            if (CommandWorker.process(client, message, false)) {
                return;
            }
        }
        if (rltn.getStatus() == Relationship.Status.Married) {
            MapleCharacter target = client.getWorldServer().getPlayerStorage().getCharacterByName(tUsername);
            if (target != null && target.getId() == player.getMarriageRing().getPartnerChrId()) {
                client.announce(MaplePacketCreator.sendSpouseChat(target, message, false));
                target.announce(MaplePacketCreator.sendSpouseChat(player, message, true));
            } else {
                player.dropMessage(5, "You are either not married or your spouse is currently offline.");
            }
        } else {
            player.dropMessage(5, "You are either not married or your spouse is currently offline.");
        }
    }
}
