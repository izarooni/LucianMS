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
package net.server.channel.handlers;

import client.*;
import client.MapleCharacter.CancelCooldownAction;
import constants.GameConstants;
import constants.skills.*;
import net.AbstractMaplePacketHandler;
import server.MapleStatEffect;
import server.TimerManager;
import server.life.MapleMonster;
import tools.FilePrinter;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

import java.awt.*;
import java.util.concurrent.ScheduledFuture;


public final class SpecialMoveHandler extends AbstractMaplePacketHandler {

    @Override
    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        MapleCharacter player = c.getPlayer();
        int skillid = slea.readInt();
        if ((!GameConstants.isPQSkillMap(c.getPlayer().getMapId()) && GameConstants.isPqSkill(skillid)) || (!c.getPlayer().isGM() && GameConstants.isGMSkills(skillid)) || (!GameConstants.isInJobTree(skillid, c.getPlayer().getJob().getId()) && !c.getPlayer().isGM())) {
            return;
        }
        Point pos = null;
        int __skillLevel = slea.readByte();
        Skill skill = SkillFactory.getSkill(skillid);
        if (skill == null) {
            return;
        }
        int skillLevel = player.getSkillLevel(skill);
        if (skillid % 10000000 == 1010 || skillid % 10000000 == 1011) {
            skillLevel = 1;
            player.setDojoEnergy(0);
            c.announce(MaplePacketCreator.getEnergy("energy", 0));
        }
        if (skillLevel == 0 || skillLevel != __skillLevel) {
            return;
        }

        MapleStatEffect effect = skill.getEffect(skillLevel);
        if ((effect.isMorph() && player.getBuffEffect(MapleBuffStat.COMBO) != null)
                || ((skill.getId() == Crusader.COMBO || skill.getId() == DawnWarrior.COMBO) && player.getBuffedValue(MapleBuffStat.MORPH) != null)) {
            return;
        }
        if (effect.getCooldown() > 0) {
            if (player.skillisCooling(skillid)) {
                return;
            } else if (skillid != Corsair.BATTLE_SHIP) {
                c.announce(MaplePacketCreator.skillCooldown(skillid, effect.getCooldown()));
                ScheduledFuture<?> timer = TimerManager.getInstance().schedule(new CancelCooldownAction(c.getPlayer(), skillid), effect.getCooldown() * 1000);
                player.addCooldown(skillid, System.currentTimeMillis(), effect.getCooldown() * 1000, timer);
            }
        }
        if (skillid == Hero.MONSTER_MAGNET || skillid == Paladin.MONSTER_MAGNET || skillid == DarkKnight.MONSTER_MAGNET) { // Monster Magnet
            int num = slea.readInt();
            int mobId;
            byte success;
            for (int i = 0; i < num; i++) {
                mobId = slea.readInt();
                success = slea.readByte();
                player.getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.showMagnet(mobId, success), false);
                MapleMonster monster = player.getMap().getMonsterByOid(mobId);
                if (monster != null) {
                    if (!monster.isBoss()) {
                        monster.switchController(c.getPlayer(), monster.isControllerHasAggro());
                    }
                }
            }
            byte direction = slea.readByte();
            player.getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.showBuffeffect(player.getId(), skillid, player.getSkillLevel(skillid), direction), false);
            c.announce(MaplePacketCreator.enableActions());
            return;
        } else if (skillid == Brawler.MP_RECOVERY) {// MP Recovery
            Skill s = SkillFactory.getSkill(skillid);
            MapleStatEffect ef = s.getEffect(player.getSkillLevel(s));
            int lose = player.getMaxHp() / ef.getX();
            player.setHp(player.getHp() - lose);
            player.updateSingleStat(MapleStat.HP, player.getHp());
            int gain = lose * (ef.getY() / 100);
            player.setMp(player.getMp() + gain);
            player.updateSingleStat(MapleStat.MP, player.getMp());
        } else if (skillid % 10000000 == 1004) {
            slea.readShort();
        }

        if (slea.available() == 5) {
            pos = new Point(slea.readShort(), slea.readShort());
        }
        if (skill.getId() == Priest.MYSTIC_DOOR && !player.isGM()) {
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        if (player.isAlive()) {
            if (skill.getId() != Priest.MYSTIC_DOOR || player.canDoor()) {
                skill.getEffect(skillLevel).applyTo(c.getPlayer(), pos);
            } else {
                player.message("Please wait 5 seconds before casting Mystic Door again");
                c.announce(MaplePacketCreator.enableActions());
            }
        } else {
            c.announce(MaplePacketCreator.enableActions());
        }
    }
}