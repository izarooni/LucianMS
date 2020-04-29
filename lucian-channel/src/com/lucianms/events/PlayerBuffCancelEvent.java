package com.lucianms.events;

import com.lucianms.client.MapleBuffStat;
import com.lucianms.client.MapleCharacter;
import com.lucianms.client.Skill;
import com.lucianms.client.SkillFactory;
import com.lucianms.constants.skills.*;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.MapleStatEffect;
import com.lucianms.server.maps.MapleSummon;
import tools.MaplePacketCreator;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
            case Aran.COMBO_ABILITY:
                player.setCombo((short) 0);
                player.cancelBuffs(Set.of(MapleBuffStat.COMBO_ABILITY_BUFF));
                break;
            default: {
                Skill skill = SkillFactory.getSkill(skillID);
                int skillLevel = player.getSkillLevel(skillID);
                if (skill != null && skillLevel > 0) {
                    MapleStatEffect effect = skill.getEffect(skillLevel);
                    if (effect != null) {
                        if (effect.isSummon()) {
                            MapleSummon remove = player.getSummons().remove(skillID);
                            if (remove != null) {
                                remove.dispose();
                        }
                        }
                        // get effects from the skill being cancelled and collect it for removal
                        Set<MapleBuffStat> remove = player.getEffects().entrySet().stream()
                                .filter(e -> effect.getStatups().containsKey(e.getKey()) && effect.isSameSource(e.getValue().getEffect()))
                                .map(Map.Entry::getKey)
                                .collect(Collectors.toSet());
                        player.cancelBuffs(remove);
                        remove.clear();
                        return null;
                    }
                }
                getLogger().warn("Skill {} has no cancel handle", skillID);
                break;
            }
        }
        return null;
    }
}