package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.events.PacketEvent;
import com.lucianms.constants.skills.*;
import tools.MaplePacketCreator;

/**
 * @author izarooni
 */
public class PlayerSkillEffectEvent extends PacketEvent {

    private int skillID;
    private int level;
    private int speed;
    private byte flags;
    private byte aids;

    @Override
    public void processInput(MaplePacketReader reader) {
        skillID = reader.readInt();
        level = reader.readByte();
        flags = reader.readByte();
        speed = reader.readByte();
        aids = reader.readByte();
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        switch (skillID) {
            case FPMage.EXPLOSION:
            case FPArchMage.BIG_BANG:
            case ILArchMage.BIG_BANG:
            case Bishop.BIG_BANG:
            case Bowmaster.HURRICANE:
            case Marksman.PIERCING_ARROW:
            case ChiefBandit.CHAKRA:
            case Brawler.CORKSCREW_BLOW:
            case Gunslinger.GRENADE:
            case Corsair.RAPID_FIRE:
            case WindArcher.HURRICANE:
            case NightWalker.POISON_BOMB:
            case ThunderBreaker.CORKSCREW_BLOW:
            case Paladin.MONSTER_MAGNET:
            case DarkKnight.MONSTER_MAGNET:
            case Hero.MONSTER_MAGNET:
            case Evan.FIRE_BREATH:
            case Evan.ICE_BREATH:
                player.getMap().broadcastMessage(player, MaplePacketCreator.skillEffect(player, skillID, level, flags, speed, aids), false);
                break;
            default:
                getLogger().warn("unhandled case for skill {} player '{}'", skillID, player.getName());
                break;
        }
        return null;
    }
}