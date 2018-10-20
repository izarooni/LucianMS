package net.server.channel.handlers;

import client.MapleCharacter;
import client.SkillFactory;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.events.PacketEvent;
import constants.skills.*;
import tools.MaplePacketCreator;

/**
 * @author izarooni
 */
public class PlayerBuffCancelEvent extends PacketEvent {

    private int skillID;

    @Override
    public void processInput(MaplePacketReader reader) {
        skillID = reader.readInt();
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        switch (skillID) {
            case FPArchMage.BIG_BANG:
            case ILArchMage.BIG_BANG:
            case Bishop.BIG_BANG:
            case Bowmaster.HURRICANE:
            case Marksman.PIERCING_ARROW:
            case Corsair.RAPID_FIRE:
            case WindArcher.HURRICANE:
            case Evan.FIRE_BREATH:
            case Evan.ICE_BREATH:
                player.getMap().broadcastMessage(player, MaplePacketCreator.skillCancel(player, skillID), false);
                break;
            default:
                player.cancelEffect(SkillFactory.getSkill(skillID).getEffect(1), false, -1);
                break;
        }
        return null;
    }
}