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
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.inventory.MapleWeaponType;
import constants.GameConstants;
import constants.skills.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scheduler.TaskExecutor;
import server.MapleStatEffect;
import server.life.FakePlayer;
import server.life.MapleMonster;
import server.maps.MapleMapObject;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.data.input.SeekableLittleEndianAccessor;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public final class CloseRangeDamageHandler extends AbstractDealDamageHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloseRangeDamageHandler.class);
    private AttackInfo attackInfo = null;

    @Override
    public void exceptionCaught(Throwable t) {
        MapleCharacter player = getClient().getPlayer();

        if (attackInfo != null) {
            for (Integer integer : attackInfo.allDamage.keySet()) {
                MapleMonster monster = player.getMap().getMonsterByOid(integer);
                if (monster != null) {
                    LOGGER.warn("Monster exception 1 {}", monster.getHp());
                    if (!monster.isAlive()) {
                    LOGGER.warn("Monster exception 2");
                        // monsters bugging out due to exceptions caused before being able to
                        // send the monster leave map packet
                        player.getMap().killMonster(monster, player, true);
                    }
                }
            }
        }
    }

    @Override
    public void process(SeekableLittleEndianAccessor slea) {
        attackInfo = parseDamage(slea, false, false);
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();

        if (player.getBuffEffect(MapleBuffStat.MORPH) != null) {
            if (player.getBuffEffect(MapleBuffStat.MORPH).isMorphWithoutAttack()) {
                // How are they attacking when the client won't let them?
                player.getClient().disconnect(false, false);
                return null;
            }
        }

        if (attackInfo.skill > 0) {
            Skill skill = SkillFactory.getSkill(attackInfo.skill);
            if (skill != null && skill.weapon > 0) {
                Item item = player.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
                if (item != null) {
                    int weaponType = MapleWeaponType.getWeaponType(item.getItemId());
                    if (weaponType != skill.weapon) {
                        // seems spear and pole arms can be used for either skills
                        if ((weaponType != 44 && weaponType != 43) && (skill.getId() >= 1311001 && skill.getId() <= 1311004)) {
                            // 44 = pole arm, 43 = spear
                            // crusher & dragon fury | spear & pole arm skills
                            return null;
                        }
                    }
                }
            }
        }

        FakePlayer fakePlayer = player.getFakePlayer();
        if (fakePlayer != null && fakePlayer.isFollowing()) {
            TaskExecutor.createTask(new Runnable() {
                @Override
                public void run() {
                    fakePlayer.getMap().broadcastMessage(fakePlayer, MaplePacketCreator.closeRangeAttack(fakePlayer, attackInfo.skill, attackInfo.skilllevel, attackInfo.stance, attackInfo.numAttackedAndDamage, attackInfo.allDamage, attackInfo.speed, attackInfo.direction, attackInfo.display), false);
                }
            }, 100);
        }
        player.getMap().broadcastMessage(player, MaplePacketCreator.closeRangeAttack(player, attackInfo.skill, attackInfo.skilllevel, attackInfo.stance, attackInfo.numAttackedAndDamage, attackInfo.allDamage, attackInfo.speed, attackInfo.direction, attackInfo.display), false, true);
        int numFinisherOrbs = 0;
        Integer comboBuff = player.getBuffedValue(MapleBuffStat.COMBO);
        if (GameConstants.isFinisherSkill(attackInfo.skill)) {
            if (comboBuff != null) {
                numFinisherOrbs = comboBuff - 1;
            }
            player.handleOrbconsume();
        } else if (attackInfo.numAttacked > 0) {
            if (attackInfo.skill != 1111008 && comboBuff != null) {
                int orbcount = player.getBuffedValue(MapleBuffStat.COMBO);
                int oid = player.isCygnus() ? DawnWarrior.COMBO_ATTACK : Crusader.COMBO_ATTACK;
                int advcomboid = player.isCygnus() ? DawnWarrior.ADVANCED_COMBO : Hero.ADVANCED_COMBO_ATTACK;
                Skill combo = SkillFactory.getSkill(oid);
                Skill advcombo = SkillFactory.getSkill(advcomboid);
                MapleStatEffect ceffect;
                int advComboSkillLevel = player.getSkillLevel(advcombo);
                if (advComboSkillLevel > 0) {
                    ceffect = advcombo.getEffect(advComboSkillLevel);
                } else {
                    ceffect = combo.getEffect(player.getSkillLevel(combo));
                }
                if (orbcount < ceffect.getX() + 1) {
                    int neworbcount = orbcount + 1;
                    if (advComboSkillLevel > 0 && ceffect.makeChanceResult()) {
                        if (neworbcount <= ceffect.getX()) {
                            neworbcount++;
                        }
                    }
                    int duration = combo.getEffect(player.getSkillLevel(oid)).getDuration();
                    List<Pair<MapleBuffStat, Integer>> stat = Collections.singletonList(new Pair<>(MapleBuffStat.COMBO, neworbcount));
                    player.setBuffedValue(MapleBuffStat.COMBO, neworbcount);
                    duration -= (int) (System.currentTimeMillis() - player.getBuffedStarttime(MapleBuffStat.COMBO));
                    getClient().announce(MaplePacketCreator.giveBuff(oid, duration, stat));
                    player.getMap().broadcastMessage(player, MaplePacketCreator.giveForeignBuff(player.getId(), stat), false);
                }
            } else if (player.getSkillLevel(player.isCygnus() ? SkillFactory.getSkill(15100004) : SkillFactory.getSkill(5110001)) > 0 && (player.getJob().isA(MapleJob.MARAUDER) || player.getJob().isA(MapleJob.THUNDERBREAKER2))) {
                for (int i = 0; i < attackInfo.numAttacked; i++) {
                    player.handleEnergyChargeGain();
                }
            }
        }
        if (attackInfo.numAttacked > 0 && attackInfo.skill == DragonKnight.SACRIFICE) {
            int totDamageToOneMonster = 0; // sacrifice attacks only 1 mob with 1 attack
            final Iterator<List<Integer>> dmgIt = attackInfo.allDamage.values().iterator();
            if (dmgIt.hasNext()) {
                totDamageToOneMonster = dmgIt.next().get(0);
            }
            int remainingHP = player.getHp() - totDamageToOneMonster * attackInfo.getAttackEffect(player, null).getX() / 100;
            if (remainingHP > 1) {
                player.setHp(remainingHP);
            } else {
                player.setHp(1);
            }
            player.updateSingleStat(MapleStat.HP, player.getHp());
            player.checkBerserk();
        }
        if (attackInfo.numAttacked > 0 && attackInfo.skill == 1211002) {
            boolean advcharge_prob = false;
            int advcharge_level = player.getSkillLevel(SkillFactory.getSkill(1220010));
            if (advcharge_level > 0) {
                advcharge_prob = SkillFactory.getSkill(1220010).getEffect(advcharge_level).makeChanceResult();
            }
            if (!advcharge_prob) {
                player.cancelEffectFromBuffStat(MapleBuffStat.WK_CHARGE);
            }
        }
        int attackCount = 1;
        if (attackInfo.skill != 0) {
            attackCount = attackInfo.getAttackEffect(player, null).getAttackCount();
        }
        if (numFinisherOrbs == 0 && GameConstants.isFinisherSkill(attackInfo.skill)) {
            return null;
        }

        addCooldown(attackInfo);

        if ((player.getSkillLevel(SkillFactory.getSkill(NightWalker.VANISH)) > 0 || player.getSkillLevel(SkillFactory.getSkill(WindArcher.WIND_WALK)) > 0 || player.getSkillLevel(SkillFactory.getSkill(Rogue.DARK_SIGHT)) > 0) && player.getBuffedValue(MapleBuffStat.DARKSIGHT) != null) {// && player.getBuffSource(MapleBuffStat.DARKSIGHT) != 9101004
            player.cancelEffectFromBuffStat(MapleBuffStat.DARKSIGHT);
            player.cancelBuffStats(MapleBuffStat.DARKSIGHT);
        }
        applyAttack(player, attackInfo, attackCount);
        if (fakePlayer != null) {
            applyAttack(fakePlayer, attackInfo, attackCount);
        }
        return null;
    }

    public AttackInfo getAttackInfo() {
        return attackInfo;
    }
}