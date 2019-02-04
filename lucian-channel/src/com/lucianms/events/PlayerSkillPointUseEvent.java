package com.lucianms.events;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleStat;
import com.lucianms.client.Skill;
import com.lucianms.client.SkillFactory;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.constants.GameConstants;
import com.lucianms.constants.skills.Aran;
import com.lucianms.events.PacketEvent;
import tools.MaplePacketCreator;

/**
 * @author izarooni
 */
public class PlayerSkillPointUseEvent extends PacketEvent {

    private int skillID;

    @Override
    public void processInput(MaplePacketReader reader) {
        reader.readInt();
        skillID = reader.readInt();
        if (skillID == Aran.HIDDEN_FULL_SWING_DOUBLE
                || skillID == Aran.HIDDEN_FULL_SWING_TRIPLE
                || skillID == Aran.HIDDEN_OVER_SWING_DOUBLE
                || skillID == Aran.HIDDEN_OVER_SWING_TRIPLE) {
            getClient().announce(MaplePacketCreator.enableActions());
            setCanceled(true);
        }
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        int remainingSp = player.getRemainingSpBySkill(GameConstants.getSkillBook(skillID / 10000));

        if ((!GameConstants.isPQSkillMap(player.getMapId()) && GameConstants.isPqSkill(skillID))
                || (!player.isGM() && GameConstants.isGMSkills(skillID))
                || (!player.isGM() && !GameConstants.isInJobTree(skillID, player.getJob().getId()))) {
            return null;
        }

        boolean beginnerSkill = false;
        if (skillID % 10000000 > 999 && skillID % 10000000 < 1003) {
            int total = 0;
            for (int i = 0; i < 3; i++) {
                total += player.getSkillLevel(SkillFactory.getSkill(player.getJobType() * 10000000 + 1000 + i));
            }
            remainingSp = Math.min((player.getLevel() - 1), 6) - total;
            beginnerSkill = true;
        }

        Skill skill = SkillFactory.getSkill(skillID);
        int curLevel = player.getSkillLevel(skill);
        if ((remainingSp > 0 && curLevel + 1 <= (skill.isFourthJob() ? player.getMasterLevel(skill) : skill.getMaxLevel()))) {
            if (!beginnerSkill) {
                player.setRemainingSp(player.getRemainingSpBySkill(GameConstants.getSkillBook(skillID / 10000)) - 1, GameConstants.getSkillBook(skillID / 10000));
            }
            player.updateSingleStat(MapleStat.AVAILABLESP, player.getRemainingSpBySkill(GameConstants.getSkillBook(skillID / 10000)));
            player.changeSkillLevel(skill, (byte) (curLevel + 1), player.getMasterLevel(skill), player.getSkillExpiration(skill));
            if (skill.getId() == Aran.DOUBLE_SWING || skill.getId() == Aran.FULL_SWING) {
                player.applyHiddenSkillFixes(skill);
            }
        }
        return null;
    }
}