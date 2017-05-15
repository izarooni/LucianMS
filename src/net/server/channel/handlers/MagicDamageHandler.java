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
import constants.skills.Bishop;
import constants.skills.Evan;
import constants.skills.FPArchMage;
import constants.skills.ILArchMage;
import server.MapleStatEffect;
import server.TimerManager;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public class MagicDamageHandler extends AbstractDealDamageHandler {

    private AttackInfo attackInfo;

    @Override
    public void process(SeekableLittleEndianAccessor slea) {
        attackInfo = parseDamage(slea, false, true);
    }

    @Override
    public void onPacket() {
        MapleCharacter player = getClient().getPlayer();
        if (player.getBuffEffect(MapleBuffStat.MORPH) != null) {
            if (player.getBuffEffect(MapleBuffStat.MORPH).isMorphWithoutAttack()) {
                // How are they attacking when the client won't let them?
                player.getClient().disconnect(false, false);
                return;
            }
        }

        byte[] packet = MaplePacketCreator.magicAttack(player, attackInfo.skill, attackInfo.skilllevel, attackInfo.stance, attackInfo.numAttackedAndDamage, attackInfo.allDamage, -1, attackInfo.speed, attackInfo.direction, attackInfo.display);
        if (attackInfo.skill == Evan.FIRE_BREATH || attackInfo.skill == Evan.ICE_BREATH || attackInfo.skill == FPArchMage.BIG_BANG || attackInfo.skill == ILArchMage.BIG_BANG || attackInfo.skill == Bishop.BIG_BANG) {
            packet = MaplePacketCreator.magicAttack(player, attackInfo.skill, attackInfo.skilllevel, attackInfo.stance, attackInfo.numAttackedAndDamage, attackInfo.allDamage, attackInfo.charge, attackInfo.speed, attackInfo.direction, attackInfo.display);
        }
        player.getMap().broadcastMessage(player, packet, false, true);
        MapleStatEffect effect = attackInfo.getAttackEffect(player, null);
        Skill skill = SkillFactory.getSkill(attackInfo.skill);
        MapleStatEffect effect_ = skill.getEffect(player.getSkillLevel(skill));
        if (effect_.getCooldown() > 0) {
            if (player.skillisCooling(attackInfo.skill)) {
                return;
            } else {
                getClient().announce(MaplePacketCreator.skillCooldown(attackInfo.skill, effect_.getCooldown()));
                player.addCooldown(attackInfo.skill, System.currentTimeMillis(), effect_.getCooldown() * 1000, TimerManager.getInstance().schedule(new CancelCooldownAction(player, attackInfo.skill), effect_.getCooldown() * 1000));
            }
        }
        applyAttack(attackInfo, effect.getAttackCount());
        Skill eaterSkill = SkillFactory.getSkill((player.getJob().getId() - (player.getJob().getId() % 10)) * 10000);// MP Eater, works with right job
        int eaterLevel = player.getSkillLevel(eaterSkill);
        if (eaterLevel > 0) {
            for (Integer singleDamage : attackInfo.allDamage.keySet()) {
                eaterSkill.getEffect(eaterLevel).applyPassive(player, player.getMap().getMapObject(singleDamage), 0);
            }
        }
    }

    public AttackInfo getAttackInfo() {
        return attackInfo;
    }
}
