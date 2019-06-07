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

import com.lucianms.client.MapleBuffStat;
import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleStat;
import com.lucianms.client.SkillFactory;
import com.lucianms.client.inventory.Item;
import com.lucianms.client.inventory.MapleInventory;
import com.lucianms.client.inventory.MapleInventoryType;
import com.lucianms.client.inventory.MapleWeaponType;
import com.lucianms.client.meta.Occupation;
import com.lucianms.constants.ItemConstants;
import com.lucianms.constants.skills.*;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.server.MapleInventoryManipulator;
import com.lucianms.server.MapleItemInformationProvider;
import com.lucianms.server.MapleStatEffect;
import com.lucianms.server.life.FakePlayer;
import tools.MaplePacketCreator;
import tools.Randomizer;

public final class PlayerDealDamageRangedEvent extends AbstractDealDamageEvent {

    private AttackInfo attackInfo;

    public PlayerDealDamageRangedEvent() {
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
        attackInfo = parseDamage(reader, true, false);
        if (attackInfo == null) {
            setCanceled(true);
        }
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        FakePlayer fakePlayer = player.getFakePlayer();

        if (player.getBuffEffect(MapleBuffStat.MORPH) != null) {
            if (player.getBuffEffect(MapleBuffStat.MORPH).isMorphWithoutAttack()) {
                // How are they attacking when the client won't let them?
                player.getClient().disconnect();
                return null;
            }
        }

        if (attackInfo.skill == Buccaneer.ENERGY_ORB || attackInfo.skill == ThunderBreaker.SPARK || attackInfo.skill == Shadower.TAUNT || attackInfo.skill == NightLord.TAUNT) {
            player.getMap().broadcastMessage(player, MaplePacketCreator.rangedAttack(player, attackInfo.skill, attackInfo.skillLevel, attackInfo.stance, attackInfo.numAttackedAndDamage, 0, attackInfo.allDamage, attackInfo.speed, attackInfo.direction, attackInfo.display), false);
            applyAttack(player, attackInfo, 1);
        } else if (attackInfo.skill == Aran.COMBO_SMASH || attackInfo.skill == Aran.COMBO_PENRIL || attackInfo.skill == Aran.COMBO_TEMPEST) {
            player.getMap().broadcastMessage(player, MaplePacketCreator.rangedAttack(player, attackInfo.skill, attackInfo.skillLevel, attackInfo.stance, attackInfo.numAttackedAndDamage, 0, attackInfo.allDamage, attackInfo.speed, attackInfo.direction, attackInfo.display), false);
            if (attackInfo.skill == Aran.COMBO_SMASH && player.getCombo() >= 30) {
                player.setCombo((short) 0);
                applyAttack(player, attackInfo, 1);
                if (fakePlayer != null) {
                    applyAttack(fakePlayer, attackInfo, 1);
                }
            } else if (attackInfo.skill == Aran.COMBO_PENRIL && player.getCombo() >= 100) {
                player.setCombo((short) 0);
                applyAttack(player, attackInfo, 2);
                if (fakePlayer != null) {
                    applyAttack(fakePlayer, attackInfo, 2);
                }
            } else if (attackInfo.skill == Aran.COMBO_TEMPEST && player.getCombo() >= 200) {
                player.setCombo((short) 0);
                applyAttack(player, attackInfo, 4);
                if (fakePlayer != null) {
                    applyAttack(fakePlayer, attackInfo, 4);
                }
            }
        } else {
            Item weapon = player.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
            MapleWeaponType type = MapleItemInformationProvider.getInstance().getWeaponType(weapon.getItemId());
            if (type == MapleWeaponType.NOT_A_WEAPON) {
                return null;
            }
            int projectile = 0;
            byte bulletCount = 1;
            MapleStatEffect effect = null;
            if (attackInfo.skill != 0) {
                effect = attackInfo.getAttackEffect(player, null);
                if (effect != null) {
                    bulletCount = effect.getBulletCount();
                    if (effect.getCooldown() > 0) {
                        getClient().announce(MaplePacketCreator.skillCooldown(attackInfo.skill, effect.getCooldown()));
                    }
                }
            }
            boolean hasShadowPartner = player.getBuffedValue(MapleBuffStat.SHADOWPARTNER) != null;
            if (hasShadowPartner) {
                bulletCount *= 2;
            }
            MapleInventory inv = player.getInventory(MapleInventoryType.USE);
            for (short i = 1; i <= inv.getSlotLimit(); i++) {
                Item item = inv.getItem(i);
                if (item != null) {
                    int id = item.getItemId();
                    boolean bow = ItemConstants.isArrowForBow(id);
                    boolean cbow = ItemConstants.isArrowForCrossBow(id);
                    if (item.getQuantity() >= bulletCount) { //Fixes the bug where you can't use your last arrow.
                        if (type == MapleWeaponType.CLAW && ItemConstants.isThrowingStar(id) && weapon.getItemId() != 1472063) {
                            if (((id != 2070007 && id != 2070018) || player.getLevel() >= 70)
                                    && (id != 2070016 || player.getLevel() >= 50)) {
                                projectile = id;
                                break;
                            }
                        } else if ((type == MapleWeaponType.GUN && ItemConstants.isBullet(id))) {
                            if (id == 2331000 && id == 2332000) {
                                if (player.getLevel() > 69) {
                                    projectile = id;
                                    break;
                                }
                            } else if (player.getLevel() > (id % 10) * 20 + 9) {
                                projectile = id;
                                break;
                            }
                        } else if ((type == MapleWeaponType.BOW && bow) || (type == MapleWeaponType.CROSSBOW && cbow) || (weapon.getItemId() == 1472063 && (bow || cbow))) {
                            projectile = id;
                            break;
                        }
                    }
                }
            }
            boolean soulArrow = player.getBuffedValue(MapleBuffStat.SOULARROW) != null;
            boolean shadowClaw = player.getBuffedValue(MapleBuffStat.SHADOW_CLAW) != null;
            if (projectile != 0) {
                if (!soulArrow && !shadowClaw && attackInfo.skill != 11101004 && attackInfo.skill != 15111007 && attackInfo.skill != 14101006) {
                    byte bulletConsume = bulletCount;

                    if (effect != null && effect.getBulletConsume() != 0) {
                        bulletConsume = (byte) (effect.getBulletConsume() * (hasShadowPartner ? 2 : 1));
                    }
                    MapleInventoryManipulator.removeById(getClient(), MapleInventoryType.USE, projectile, bulletConsume, false, true);
                }
            }
            if (projectile != 0 || soulArrow || attackInfo.skill == 11101004 || attackInfo.skill == 15111007 || attackInfo.skill == 14101006) {
                int visProjectile = projectile; //visible projectile sent to players
                if (ItemConstants.isThrowingStar(projectile)) {
                    MapleInventory cash = player.getInventory(MapleInventoryType.CASH);
                    for (int i = 1; i <= cash.getSlotLimit(); i++) { // impose order...
                        Item item = cash.getItem((short) i);
                        if (item != null) {
                            if (item.getItemId() / 1000 == 5021) {
                                visProjectile = item.getItemId();
                                break;
                            }
                        }
                    }
                } else //bow, crossbow
                    if (soulArrow || attackInfo.skill == 3111004 || attackInfo.skill == 3211004 || attackInfo.skill == 11101004 || attackInfo.skill == 15111007 || attackInfo.skill == 14101006) {
                        visProjectile = 0;
                    }
                byte[] packet;
                switch (attackInfo.skill) {
                    case 3121004: // Hurricane
                    case 3221001: // Pierce
                    case 5221004: // Rapid Fire
                    case 13111002: // KoC Hurricane
                        packet = MaplePacketCreator.rangedAttack(player, attackInfo.skill, attackInfo.skillLevel, attackInfo.rangeDirection, attackInfo.numAttackedAndDamage, visProjectile, attackInfo.allDamage, attackInfo.speed, attackInfo.direction, attackInfo.display);
                        break;
                    default:
                        packet = MaplePacketCreator.rangedAttack(player, attackInfo.skill, attackInfo.skillLevel, attackInfo.stance, attackInfo.numAttackedAndDamage, visProjectile, attackInfo.allDamage, attackInfo.speed, attackInfo.direction, attackInfo.display);
                        break;
                }

                if (fakePlayer != null && fakePlayer.isFollowing()) {
                    int finalVisProjectile = visProjectile;
                    TaskExecutor.createTask(() -> fakePlayer.getMap().broadcastMessage(fakePlayer, MaplePacketCreator.rangedAttack(fakePlayer, attackInfo.skill, attackInfo.skillLevel, attackInfo.stance, attackInfo.numAttackedAndDamage, finalVisProjectile, attackInfo.allDamage, attackInfo.speed, attackInfo.direction, attackInfo.display), false, true), 100);
                }

                player.getMap().broadcastMessage(player, packet, false, true);
                if (effect != null) {
                    int money = effect.getMoneyCon();
                    if (money != 0) {
                        int moneyMod = money / 2;
                        money += Randomizer.nextInt(moneyMod);
                        if (money > player.getMeso()) {
                            money = player.getMeso();
                        }
                        player.gainMeso(-money, false);
                    }
                }
                addCooldown(attackInfo);
                if ((player.getSkillLevel(SkillFactory.getSkill(NightWalker.VANISH)) > 0 || player.getSkillLevel(SkillFactory.getSkill(WindArcher.WIND_WALK)) > 0) && player.getBuffedValue(MapleBuffStat.DARKSIGHT) != null && attackInfo.numAttacked > 0 && player.getBuffSource(MapleBuffStat.DARKSIGHT) != 9101004) {
                    player.cancelEffectFromBuffStat(MapleBuffStat.DARKSIGHT);
                    player.cancelBuffStats(MapleBuffStat.DARKSIGHT);
                }
                applyAttack(player, attackInfo, bulletCount);
                if (fakePlayer != null) {
                    applyAttack(fakePlayer, attackInfo, bulletCount);
                }
            }
        }
        return null;
    }

    public AttackInfo getAttackInfo() {
        return attackInfo;
    }
}