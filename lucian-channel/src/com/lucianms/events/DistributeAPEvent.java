/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
		       Matthias Butz <matze@odinms.de>
		       Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.lucianms.events;

import com.lucianms.client.*;
import com.lucianms.constants.skills.*;
import com.lucianms.nio.receive.MaplePacketReader;
import tools.MaplePacketCreator;

public final class DistributeAPEvent extends PacketEvent {

    private int stat;

    @Override
    public void processInput(MaplePacketReader reader) {
        reader.readInt();
        stat = reader.readInt();
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        if (player.getRemainingAp() > 0) {
            if (addStat(player, stat)) {
                player.setRemainingAp(player.getRemainingAp() - 1);
                player.updateSingleStat(MapleStat.AVAILABLEAP, player.getRemainingAp());
            }
        }
        player.announce(MaplePacketCreator.enableActions());
        return null;
    }

    static boolean addStat(MapleCharacter player, int stat) {
        switch (stat) {
            case 64: // Str
                if (player.getStr() < Short.MAX_VALUE) {
                    player.addStat(1, 1);
                }
                break;
            case 128: // Dex
                if (player.getDex() < Short.MAX_VALUE) {
                    player.addStat(2, 1);
                }
                break;
            case 256: // Int
                if (player.getInt() < Short.MAX_VALUE) {
                    player.addStat(3, 1);
                }
                break;
            case 512: // Luk
                if (player.getLuk() < Short.MAX_VALUE) {
                    player.addStat(4, 1);
                }
                break;
            case 2048: // HP
//                addHP(player, addHP(player));
//                break;
            case 8192: // MP
//                addMP(player, addMP(player));
                player.dropMessage(1, "Distributing AP into HP and MP is currently disabled");
                return false;
            default:
                player.announce(MaplePacketCreator.updatePlayerStats(MaplePacketCreator.EMPTY_STATUPDATE, true, player));
                break;
        }
        return true;
    }

    private static int addHP(MapleCharacter player) {
        MapleJob job = player.getJob();
        int MaxHP = player.getMaxHp();
        if (player.getHpMpApUsed() > 9999 || MaxHP >= 30000) {
            return MaxHP;
        }
        if (job.isA(MapleJob.WARRIOR) || job.isA(MapleJob.DAWNWARRIOR1) || job.isA(MapleJob.ARAN1)) {
            Skill increaseHP = SkillFactory.getSkill(job.isA(MapleJob.DAWNWARRIOR1) ? DawnWarrior.MAX_HP_ENHANCEMENT : Warrior.IMPROVED_MAXHP_INCREASE);
            int sLvl = player.getSkillLevel(increaseHP);

            if (sLvl > 0)
                MaxHP += increaseHP.getEffect(sLvl).getY();

            MaxHP += 20;
        } else if (job.isA(MapleJob.MAGICIAN) || job.isA(MapleJob.BLAZEWIZARD1)) {
            MaxHP += 6;
        } else if (job.isA(MapleJob.BOWMAN) || job.isA(MapleJob.WINDARCHER1) || job.isA(MapleJob.THIEF) || job.isA(MapleJob.NIGHTWALKER1)) {
            MaxHP += 16;
        } else if (job.isA(MapleJob.PIRATE) || job.isA(MapleJob.THUNDERBREAKER1)) {
            Skill increaseHP = SkillFactory.getSkill(Brawler.IMPROVE_MAXHP);
            int sLvl = player.getSkillLevel(increaseHP);

            if (sLvl > 0)
                MaxHP += increaseHP.getEffect(sLvl).getY();

            MaxHP += 18;
        } else {
            MaxHP += 8;
        }
        return MaxHP;
    }

    private static int addMP(MapleCharacter player) {
        int MaxMP = player.getMaxMp();
        MapleJob job = player.getJob();
        if (player.getHpMpApUsed() > 9999 || player.getMaxMp() >= 30000) {
            return MaxMP;
        }
        if (job.isA(MapleJob.WARRIOR) || job.isA(MapleJob.DAWNWARRIOR1) || job.isA(MapleJob.ARAN1)) {
            MaxMP += 2;
        } else if (job.isA(MapleJob.MAGICIAN) || job.isA(MapleJob.BLAZEWIZARD1)) {
            Skill increaseMP = SkillFactory.getSkill(job.isA(MapleJob.BLAZEWIZARD1) ? BlazeWizard.INCREASING_MAX_MP : Magician.IMPROVED_MAXMP_INCREASE);
            int sLvl = player.getSkillLevel(increaseMP);

            if (sLvl > 0) {
                MaxMP += increaseMP.getEffect(sLvl).getY();
            }
            MaxMP += 18;
        } else if (job.isA(MapleJob.BOWMAN) || job.isA(MapleJob.WINDARCHER1) || job.isA(MapleJob.THIEF) || job.isA(MapleJob.NIGHTWALKER1)) {
            MaxMP += 10;
        } else if (job.isA(MapleJob.PIRATE) || job.isA(MapleJob.THUNDERBREAKER1)) {
            MaxMP += 14;
        } else {
            MaxMP += 6;
        }
        return MaxMP;
    }

    private static void addHP(MapleCharacter player, int MaxHP) {
        MaxHP = Math.min(30000, MaxHP);
        player.setHpMpApUsed(player.getHpMpApUsed() + 1);
        player.setMaxHp(MaxHP);
        player.updateSingleStat(MapleStat.MAXHP, MaxHP);
    }

    private static void addMP(MapleCharacter player, int MaxMP) {
        MaxMP = Math.min(30000, MaxMP);
        player.setHpMpApUsed(player.getHpMpApUsed() + 1);
        player.setMaxMp(MaxMP);
        player.updateSingleStat(MapleStat.MAXMP, MaxMP);
    }
}
