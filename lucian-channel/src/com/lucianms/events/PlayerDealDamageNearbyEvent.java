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
import com.lucianms.client.inventory.Item;
import com.lucianms.client.inventory.MapleInventoryType;
import com.lucianms.client.inventory.MapleWeaponType;
import com.lucianms.client.meta.Occupation;
import com.lucianms.constants.GameConstants;
import com.lucianms.constants.skills.*;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.server.BuffContainer;
import com.lucianms.server.MapleStatEffect;
import com.lucianms.server.life.FakePlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.MaplePacketCreator;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PlayerDealDamageNearbyEvent extends AbstractDealDamageEvent {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerDealDamageNearbyEvent.class);
    private AttackInfo attackInfo = null;

    public PlayerDealDamageNearbyEvent() {
        onPost(new Runnable() {
            @Override
            public void run() {
                if (!isCanceled()) {
                    MapleCharacter player = getClient().getPlayer();
                    Occupation occupation = player.getOccupation();
                    if (occupation != null && occupation.getType() == Occupation.Type.Undead
                            && attackInfo != null && !attackInfo.allDamage.isEmpty()) {
                        int gain = (int) (player.getMaxHp() * Math.random() * 0.07 + 0.03);
                        player.addHP(gain);
                        player.updateSingleStat(MapleStat.HP, player.getHp());
                    }
                }
            }
        });
    }

    @Override
    public void processInput(MaplePacketReader reader) {
        attackInfo = parseDamage(reader, false, false);
        if (attackInfo == null) {
            setCanceled(true);
        }
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();

        BuffContainer morph = player.getEffects().get(MapleBuffStat.MORPH);
        if (morph != null && morph.getEffect().isMorphWithoutAttack()) {
            return null;
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
                    fakePlayer.getMap().broadcastMessage(fakePlayer, MaplePacketCreator.closeRangeAttack(fakePlayer, attackInfo.skill, attackInfo.skillLevel, attackInfo.stance, attackInfo.numAttackedAndDamage, attackInfo.allDamage, attackInfo.speed, attackInfo.direction, attackInfo.display), false);
                }
            }, 100);
        }
        player.getMap().broadcastMessage(player, MaplePacketCreator.closeRangeAttack(player, attackInfo.skill, attackInfo.skillLevel, attackInfo.stance, attackInfo.numAttackedAndDamage, attackInfo.allDamage, attackInfo.speed, attackInfo.direction, attackInfo.display), false, true);
        int numFinisherOrbs = 0;
        Integer comboBuff = player.getBuffedValue(MapleBuffStat.COMBO_COUNTER);
        if (GameConstants.isFinisherSkill(attackInfo.skill)) {
            if (comboBuff != null) {
                numFinisherOrbs = comboBuff - 1;
            }
            player.handleOrbconsume();
        } else if (attackInfo.numAttacked > 0) {
            if (attackInfo.skill != 1111008 && comboBuff != null) {
                int orbcount = player.getBuffedValue(MapleBuffStat.COMBO_COUNTER);
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
                    MapleStatEffect effect = combo.getEffect(player.getSkillLevel(oid));
                    BuffContainer container = new BuffContainer(effect, null, System.currentTimeMillis(), neworbcount);
                    player.getEffects().put(MapleBuffStat.COMBO_COUNTER, container);
                    Map<MapleBuffStat, BuffContainer> map = Map.of(MapleBuffStat.COMBO_COUNTER, container);
                    player.announce(MaplePacketCreator.setTempStats(map));
                    player.getMap().sendPacketExclude(MaplePacketCreator.setRemoteTempStats(player, map), player);
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
                player.cancelBuffs(Set.of(MapleBuffStat.WEAPON_CHARGE));
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

        if ((player.getSkillLevel(SkillFactory.getSkill(NightWalker.VANISH)) > 0 || player.getSkillLevel(SkillFactory.getSkill(WindArcher.WIND_WALK)) > 0 || player.getSkillLevel(SkillFactory.getSkill(Rogue.DARK_SIGHT)) > 0) && player.getBuffedValue(MapleBuffStat.DARK_SIGHT) != null) {// && player.getBuffSource(MapleBuffStat.DARKSIGHT) != 9101004
            player.cancelBuffs(Set.of(MapleBuffStat.DARK_SIGHT));
            player.cancelBuffStats(MapleBuffStat.DARK_SIGHT);
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