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
import com.lucianms.client.inventory.Equip;
import com.lucianms.client.inventory.Item;
import com.lucianms.client.inventory.MapleInventoryType;
import com.lucianms.client.status.MonsterStatus;
import com.lucianms.client.status.MonsterStatusEffect;
import com.lucianms.constants.GameConstants;
import com.lucianms.constants.ItemConstants;
import com.lucianms.constants.ServerConstants;
import com.lucianms.constants.skills.*;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.server.BuffContainer;
import com.lucianms.server.MapleItemInformationProvider;
import com.lucianms.server.MapleStatEffect;
import com.lucianms.server.life.*;
import com.lucianms.server.maps.MapleMap;
import com.lucianms.server.maps.MapleMapItem;
import com.lucianms.server.maps.MapleMapObject;
import com.lucianms.server.maps.MapleMapObjectType;
import com.lucianms.server.partyquest.Pyramid;
import tools.MaplePacketCreator;
import tools.Randomizer;

import java.awt.*;
import java.util.List;
import java.util.*;

public abstract class AbstractDealDamageEvent extends PacketEvent {

    @Override
    public void exceptionCaught(MaplePacketReader reader, Throwable t) {
        AttackInfo attackInfo = getAttackInfo();
        if (attackInfo != null) {
            getLogger().error("{}", attackInfo.toString());
        }
        super.exceptionCaught(reader, t);
    }

    public abstract AttackInfo getAttackInfo();

    synchronized void applyAttack(MapleCharacter player, AttackInfo attack, int attackCount) {
        Skill theSkill = null;
        MapleStatEffect attackEffect = null;
        final int job = player.getJob().getId();
        try {
            if (player.isBanned()) {
                return;
            }
            if (attack.skill != 0) {
                theSkill = SkillFactory.getSkill(GameConstants.getHiddenSkill(attack.skill)); //returns back the skill id if its not a hidden skill so we are gucci
                attackEffect = attack.getAttackEffect(player, theSkill);
                if (attackEffect == null) {
                    player.getClient().announce(MaplePacketCreator.enableActions());
                    return;
                }

                if (attack.skill != Cleric.HEAL) {
                    if (player.isAlive()) {
                        if (attack.skill == NightWalker.POISON_BOMB) // Poison Bomb
                        {
                            attackEffect.applyTo(player, new Point(attack.position.x, attack.position.y));
                        } else {
                            attackEffect.applyTo(player);
                        }
                    } else {
                        player.getClient().announce(MaplePacketCreator.enableActions());
                    }
                }
            }
            if (!player.isAlive()) {
                return;
            }

            long totDamage = 0;
            final MapleMap map = player.getMap();

            if (attack.skill == ChiefBandit.MESO_EXPLOSION) {
                int delay = 0;
                for (Integer oned : attack.allDamage.keySet()) {
                    MapleMapObject mapobject = map.getMapObject(oned);
                    if (mapobject != null && mapobject.getType() == MapleMapObjectType.ITEM) {
                        final MapleMapItem mapitem = (MapleMapItem) mapobject;
                        if (mapitem.getMeso() == 0) { //Maybe it is possible some how?
                            return;
                        }
                        synchronized (mapitem) {
                            if (mapitem.isPickedUp()) {
                                return;
                            }
                            TaskExecutor.createTask(new Runnable() {
                                @Override
                                public void run() {
                                    map.removeMapObject(mapitem);
                                    map.broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 4, 0), mapitem.getPosition());
                                    mapitem.setPickedUp(true);
                                }
                            }, delay);
                            delay += 100;
                        }
                    } else if (mapobject != null && mapobject.getType() != MapleMapObjectType.MONSTER) {
                        return;
                    }
                }
            }
            for (Map.Entry<Integer, List<Integer>> entry : attack.allDamage.entrySet()) {
                final MapleMonster monster = map.getMonsterByOid(entry.getKey());
                List<Integer> damageLines = entry.getValue();
                if (player.isDebug()) {
                    damageLines.replaceAll(i -> Integer.MAX_VALUE);
                }
                if (monster != null) {
                    long totalDamage = damageLines.stream().mapToLong(Integer::intValue).sum();
                    player.checkMonsterAggro(monster);

                    if (player.getBuffedValue(MapleBuffStat.PICK_POCKET) != null
                            && (attack.skill == 0 || attack.skill == Rogue.DOUBLE_STAB || attack.skill == Bandit.SAVAGE_BLOW || attack.skill == ChiefBandit.ASSAULTER || attack.skill == ChiefBandit.BAND_OF_THIEVES || attack.skill == Shadower.ASSASSINATE || attack.skill == Shadower.TAUNT || attack.skill == Shadower.BOOMERANG_STEP)) {
                        Skill pickpocket = SkillFactory.getSkill(ChiefBandit.PICK_POCKET);
                        int delay = 0;
                        final int maxMeso = player.getBuffedValue(MapleBuffStat.PICK_POCKET);
                        for (Integer damage : damageLines) {

                            damage += Integer.MAX_VALUE;
                            if (pickpocket.getEffect(player.getSkillLevel(pickpocket)).makeChanceResult()) {
                                final Integer eachdf;
                                if (damage < 0) {
                                    eachdf = damage + Integer.MAX_VALUE;
                                } else {
                                    eachdf = damage;
                                }

                                int dropSize = Math.min((int) Math.max(((double) eachdf / (double) 20000) * (double) maxMeso, (double) 1), maxMeso);
                                Point dropLocation = monster.getPosition().getLocation();
                                dropLocation.x += Randomizer.rand(-50, 50);

                                TaskExecutor.createTask(() -> player.getMap().spawnMesoDrop(dropSize, dropLocation, monster, player, true, (byte) 2), delay);
                                delay += 100;
                            }
                        }
                    } else if (attack.skill == Marauder.ENERGY_DRAIN || attack.skill == ThunderBreaker.ENERGY_DRAIN || attack.skill == NightWalker.VAMPIRE || attack.skill == Assassin.DRAIN) {
                        Optional<SkillEntry> skill = player.getSkill(attack.skill);
                        if (skill.isPresent()) {
                            int maxHP = (int) Math.max(MapleCharacter.MAX_HEALTH, monster.getMaxHp());
                            long localDamage = totDamage * SkillFactory.getSkill(attack.skill).getEffect(skill.get().getLevel()).getX() / 100;
                            player.addHP((int) Math.min(maxHP, Math.min(localDamage, player.getHp() / 2)));
                        }
                    } else if (attack.skill == Bandit.STEAL) {
                        Skill steal = SkillFactory.getSkill(Bandit.STEAL);
                        if (monster.getStolen().size() < 1) { // One steal per mob <3
                            if (Math.random() < 0.3 && steal.getEffect(player.getSkillLevel(steal)).makeChanceResult()) { //Else it drops too many cool stuff :(
                                List<MonsterDropEntry> toSteals = MapleMonsterInformationProvider.retrieveDrop(monster.getId());
                                Collections.shuffle(toSteals);
                                int toSteal = toSteals.get(Randomizer.rand(0, (toSteals.size() - 1))).itemId;
                                MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                                Item item;
                                if (ItemConstants.getInventoryType(toSteal).equals(MapleInventoryType.EQUIP)) {
                                    item = ii.randomizeStats(ii.getEquipById(toSteal));
                                } else {
                                    item = new Item(toSteal, (byte) 0, (short) 1, -1);
                                }
                                player.getMap().spawnItemDrop(monster, player, item, monster.getPosition(), false, false);
                                monster.addStolen(toSteal);
                            }
                        }
                    } else if (attack.skill == FPArchMage.FIRE_DEMON) {
                        monster.setTempEffectiveness(Element.ICE, ElementalEffectiveness.WEAK, SkillFactory.getSkill(FPArchMage.FIRE_DEMON).getEffect(player.getSkillLevel(SkillFactory.getSkill(FPArchMage.FIRE_DEMON))).getDuration() * 1000);
                    } else if (attack.skill == ILArchMage.ICE_DEMON) {
                        monster.setTempEffectiveness(Element.FIRE, ElementalEffectiveness.WEAK, SkillFactory.getSkill(ILArchMage.ICE_DEMON).getEffect(player.getSkillLevel(SkillFactory.getSkill(ILArchMage.ICE_DEMON))).getDuration() * 1000);
                    } else if (attack.skill == Outlaw.HOMING_BEACON || attack.skill == Corsair.BULLSEYE) {
                        player.setMarkedMonster(monster.getObjectId());
                        player.announce(MaplePacketCreator.giveBuff(1, attack.skill, Map.of(MapleBuffStat.GUIDED_BULLET, monster.getObjectId())));
                    }

                    if (job == 2111 || job == 2112) {
                        if (player.getBuffedValue(MapleBuffStat.WEAPON_CHARGE) != null) {
                            Skill snowCharge = SkillFactory.getSkill(Aran.SNOW_CHARGE);
                            if (totalDamage > 0) {
                                MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.SPEED, snowCharge.getEffect(player.getSkillLevel(snowCharge)).getX()), snowCharge, null, false);
                                monster.applyStatus(player, monsterStatusEffect, false, snowCharge.getEffect(player.getSkillLevel(snowCharge)).getY() * 1000);
                            }
                        }
                    }
                    if (player.getBuffedValue(MapleBuffStat.HAMSTRING) != null) {
                        Skill hamstring = SkillFactory.getSkill(Bowmaster.HAMSTRING);
                        if (hamstring.getEffect(player.getSkillLevel(hamstring)).makeChanceResult()) {
                            MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.SPEED, hamstring.getEffect(player.getSkillLevel(hamstring)).getX()), hamstring, null, false);
                            monster.applyStatus(player, monsterStatusEffect, false, hamstring.getEffect(player.getSkillLevel(hamstring)).getY() * 1000);
                        }
                    }
                    if (player.getBuffedValue(MapleBuffStat.SLOW) != null) {
                        Skill slow = SkillFactory.getSkill(Evan.SLOW);
                        if (slow.getEffect(player.getSkillLevel(slow)).makeChanceResult()) {
                            MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.SPEED, slow.getEffect(player.getSkillLevel(slow)).getX()), slow, null, false);
                            monster.applyStatus(player, monsterStatusEffect, false, slow.getEffect(player.getSkillLevel(slow)).getY() * 60 * 1000);
                        }
                    }
                    if (player.getBuffedValue(MapleBuffStat.BLIND) != null) {
                        Skill blind = SkillFactory.getSkill(Marksman.BLIND);
                        if (blind.getEffect(player.getSkillLevel(blind)).makeChanceResult()) {
                            MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.ACC, blind.getEffect(player.getSkillLevel(blind)).getX()), blind, null, false);
                            monster.applyStatus(player, monsterStatusEffect, false, blind.getEffect(player.getSkillLevel(blind)).getY() * 1000);
                        }
                    }
                    if (job == 121 || job == 122) {
                        for (int charge = 1211005; charge < 1211007; charge++) {
                            Skill chargeSkill = SkillFactory.getSkill(charge);
                            if (player.isBuffFrom(MapleBuffStat.WEAPON_CHARGE, chargeSkill)) {
                                if (totalDamage > 0) {
                                    monster.setTempEffectiveness(Element.ICE, ElementalEffectiveness.WEAK, chargeSkill.getEffect(player.getSkillLevel(chargeSkill)).getY() * 1000);
                                    break;
                                }
                            }
                        }
                        if (job == 122) {
                            for (int charge = 1221003; charge < 1221004; charge++) {
                                Skill chargeSkill = SkillFactory.getSkill(charge);
                                if (player.isBuffFrom(MapleBuffStat.WEAPON_CHARGE, chargeSkill)) {
                                    if (totalDamage > 0) {
                                        monster.setTempEffectiveness(Element.HOLY, ElementalEffectiveness.WEAK, chargeSkill.getEffect(player.getSkillLevel(chargeSkill)).getY() * 1000);
                                        break;
                                    }
                                }
                            }
                        }
                    } else if (player.getBuffedValue(MapleBuffStat.COMBO_DRAIN) != null) {
                        Skill skill;
                        if (player.getBuffedValue(MapleBuffStat.COMBO_DRAIN) != null) {
                            skill = SkillFactory.getSkill(21100005);
                            /*int gainhp;
                            gainhp = (int) ((double) Math.min(30000, 50000) * (double) skill.getEffect(player.getSkillLevel(skill)).getX() / 100.0);
                            gainhp = Math.min((int)monster.getMaxHp(), Math.min(gainhp, player.getMaxHp() / 10));
                            player.addHP(gainhp);

                             */
                            // totDamage = attack.numDamage;
                            long decHP = totalDamage * skill.getEffect(player.getSkillLevel(skill.getId())).getX() / 1500;
                            int currentHealth = player.getHp();
                            player.addHP((int) Math.max(1, decHP - currentHealth));
                            player.updateSingleStat(MapleStat.HP, player.getHp());


                        }
                    } else if (job == 412 || job == 422 || job == 1411) {
                        Skill type = SkillFactory.getSkill(player.getJob().getId() == 412 ? 4120005 : (player.getJob().getId() == 1411 ? 14110004 : 4220005));
                        if (player.getSkillLevel(type) > 0) {
                            MapleStatEffect venomEffect = type.getEffect(player.getSkillLevel(type));
                            for (int i = 0; i < attackCount; i++) {
                                if (venomEffect.makeChanceResult()) {
                                    if (monster.getVenomMulti() < 3) {
                                        monster.setVenomMulti((monster.getVenomMulti() + 1));
                                        MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.POISON, 1), type, null, false);
                                        monster.applyStatus(player, monsterStatusEffect, false, venomEffect.getDuration(), true);
                                    }
                                }
                            }
                        }
                    } else if (job == 521 || job == 522) { // from what I can gather this is how it should work
                        if (!monster.isBoss()) {
                            Skill type = SkillFactory.getSkill(Outlaw.FLAMETHROWER);
                            if (player.getSkillLevel(type) > 0) {
                                MapleStatEffect DoT = type.getEffect(player.getSkillLevel(type));
                                MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.POISON, 1), type, null, false);
                                monster.applyStatus(player, monsterStatusEffect, true, DoT.getDuration(), false);
                            }
                        }
                    } else if (job >= 311 && job <= 322) {
                        if (!monster.isBoss()) {
                            Skill mortalBlow;
                            if (job == 311 || job == 312) {
                                mortalBlow = SkillFactory.getSkill(Ranger.MORTAL_BLOW);
                            } else {
                                mortalBlow = SkillFactory.getSkill(Sniper.MORTAL_BLOW);
                            }
                            if (player.getSkillLevel(mortalBlow) > 0) {
                                MapleStatEffect mortal = mortalBlow.getEffect(player.getSkillLevel(mortalBlow));
                                if (monster.getHp() <= (monster.getStats().getHp() * mortal.getX()) / 100) {
                                    if (Randomizer.rand(1, 100) <= mortal.getY()) {
                                        monster.getMap().killMonster(monster, player, true);
                                    }
                                }
                            }
                        }
                    }
                    if (totalDamage > 0 && attackEffect != null && attackEffect.getMonsterStati().size() > 0) {
                        if (attackEffect.makeChanceResult()) {
                            monster.applyStatus(player, new MonsterStatusEffect(attackEffect.getMonsterStati(), theSkill, null, false), attackEffect.isPoison(), attackEffect.getDuration());
                        }
                    }
                    if (attack.isHH && monster.isBoss()) {
                        map.damageMonster(player, monster, ((long) monster.getHp() / 1000));
                    } else if (attack.isHH && !monster.isBoss()) {
                        int HHDmg = (player.calculateMaxBaseDamage(player.getTotalWatk()) * (SkillFactory.getSkill(Paladin.HEAVENS_HAMMER).getEffect(player.getSkillLevel(SkillFactory.getSkill(Paladin.HEAVENS_HAMMER))).getDamage() / 100));
                        map.damageMonster(player, monster, (int) (Math.floor(Math.random() * (HHDmg / 5f) + HHDmg * .8)));
                    } else if (attack.isTempest && !monster.isBoss()) {
                        map.damageMonster(player, monster, monster.getHp());
                    } else if (attack.isTempest) {
                        int TmpDmg = (player.calculateMaxBaseDamage(player.getTotalWatk()) * (SkillFactory.getSkill(Aran.COMBO_TEMPEST).getEffect(player.getSkillLevel(SkillFactory.getSkill(Aran.COMBO_TEMPEST))).getDamage() / 100));
                        map.damageMonster(player, monster, (int) (Math.floor(Math.random() * (TmpDmg / 5f) + TmpDmg * .8)));
                    } else {
                        map.damageMonster(player, monster, totalDamage);
                    }
                    if (monster.isBuffed(MonsterStatus.WEAPON_REFLECT)) {
                        for (int i = 0; i < monster.getSkills().size(); i++) {
                            if (monster.getSkills().get(i).left == 145) {
                                MobSkill toUse = MobSkillFactory.getMobSkill(monster.getSkills().get(i).left, monster.getSkills().get(i).right);
                                player.addHP(-toUse.getX());
                                map.broadcastMessage(player, MaplePacketCreator.damagePlayer(0, monster.getId(), player.getId(), toUse.getX(), 0, 0, false, 0, true, monster.getObjectId(), 0, 0), true);
                            }
                        }
                    }
                    if (monster.isBuffed(MonsterStatus.MAGIC_REFLECT)) {
                        for (int i = 0; i < monster.getSkills().size(); i++) {
                            if (monster.getSkills().get(i).left == 145) {
                                MobSkill toUse = MobSkillFactory.getMobSkill(monster.getSkills().get(i).left, monster.getSkills().get(i).right);
                                player.addMP(-toUse.getY());
                            }
                        }
                    }
                    Equip weapon = player.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
                    if (weapon != null && weapon.isRegalia() && monster.getDamageTask() == null) {
                        monster.applyDamageOvertime(player, 5 * 1000);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    AttackInfo parseDamage(MaplePacketReader reader, boolean ranged, boolean magic) {
        MapleCharacter player = getClient().getPlayer();
        //2C 00 00 01 91 A1 12 00 A5 57 62 FC E2 75 99 10 00 47 80 01 04 01 C6 CC 02 DD FF 5F 00
        AttackInfo ret = new AttackInfo();
        reader.readByte();
        ret.numAttackedAndDamage = reader.readByte();
        ret.numAttacked = (ret.numAttackedAndDamage >>> 4) & 0xF;
        ret.numDamage = ret.numAttackedAndDamage & 0xF;
        ret.allDamage = new HashMap<>();
        ret.skill = reader.readInt();
        ret.ranged = ranged;
        ret.magic = magic;
        if (ret.skill > 0) {
            ret.skillLevel = player.getSkillLevel(ret.skill);
        }
        if (ret.skill == Evan.ICE_BREATH || ret.skill == Evan.FIRE_BREATH || ret.skill == FPArchMage.BIG_BANG || ret.skill == ILArchMage.BIG_BANG || ret.skill == Bishop.BIG_BANG || ret.skill == Gunslinger.GRENADE || ret.skill == Brawler.CORKSCREW_BLOW || ret.skill == ThunderBreaker.CORKSCREW_BLOW || ret.skill == NightWalker.POISON_BOMB) {
            ret.charge = reader.readInt();
        } else {
            ret.charge = 0;
        }
        if (ret.skill == Paladin.HEAVENS_HAMMER) {
            ret.isHH = true;
        } else if (ret.skill == Aran.COMBO_TEMPEST) {
            ret.isTempest = true;
        }

        //if (ret.skill > 0 && getClient().getPlayer().getMapId() == ServerConstants.HOME_MAP  ) {
        if (ret.skill > 0 && cantUseSkill()) {
            SpamTracker.SpamData spamTracker = player.getSpamTracker(SpamTracker.SpamOperation.SkillUsage);
            if (!spamTracker.testFor(8000)) {
                player.sendMessage(5, "Skills are disabled in this map and will not show for other players.");
                spamTracker.record();
            }
            setCanceled(true);
            return null;
        }

        reader.skip(8);
        ret.display = reader.readByte();
        ret.direction = reader.readByte();
        ret.stance = reader.readByte();
        if (ret.skill == ChiefBandit.MESO_EXPLOSION) {
            if (ret.numAttackedAndDamage == 0) {
                reader.skip(10);
                int bullets = reader.readByte();
                for (int j = 0; j < bullets; j++) {
                    int mesoid = reader.readInt();
                    reader.skip(1);
                    ret.allDamage.put(mesoid, null);
                }
                return ret;
            } else {
                reader.skip(6);
            }
            for (int i = 0; i < ret.numAttacked + 1; i++) {
                int oid = reader.readInt();
                if (i < ret.numAttacked) {
                    reader.skip(12);
                    int bullets = reader.readByte();
                    List<Integer> allDamageNumbers = new ArrayList<>();
                    for (int j = 0; j < bullets; j++) {
                        allDamageNumbers.add(reader.readInt());
                    }
                    ret.allDamage.put(oid, allDamageNumbers);
                    reader.skip(4);
                } else {
                    int bullets = reader.readByte();
                    for (int j = 0; j < bullets; j++) {
                        int mesoid = reader.readInt();
                        reader.skip(1);
                        ret.allDamage.put(mesoid, null);
                    }
                }
            }
            return ret;
        }
        if (ranged) {
            reader.readByte();
            ret.speed = reader.readByte();
            reader.readByte();
            ret.rangeDirection = reader.readByte();
            reader.skip(7);
            if (ret.skill == Bowmaster.HURRICANE || ret.skill == Marksman.PIERCING_ARROW || ret.skill == Corsair.RAPID_FIRE || ret.skill == WindArcher.HURRICANE) {
                reader.skip(4);
            }
        } else {
            reader.readByte();
            ret.speed = reader.readByte();
            reader.skip(4);
        }
        long calcDmgMax;

        // Find the base damage to base futher calculations on.
        // Several skills have their own formula in this section.
        if (magic && ret.skill != 0) {
            calcDmgMax = (player.getTotalMagic() * player.getTotalMagic() / 1000 + player.getTotalMagic()) / 30 + player.getTotalInt() / 200;
        } else if (ret.skill == 4001344 || ret.skill == NightWalker.LUCKY_SEVEN || ret.skill == NightLord.TRIPLE_THROW) {
            calcDmgMax = (player.getTotalLuk() * 5) * player.getTotalWatk() / 100;
        } else if (ret.skill == DragonKnight.DRAGON_ROAR) {
            calcDmgMax = (player.getTotalStr() * 4 + player.getTotalDex()) * player.getTotalWatk() / 100;
        } else if (ret.skill == NightLord.VENOMOUS_STAR || ret.skill == Shadower.VENOMOUS_STAB) {
            calcDmgMax = (int) (18.5 * (player.getTotalStr() + player.getTotalLuk()) + player.getTotalDex() * 2) / 100 * player.calculateMaxBaseDamage(player.getTotalWatk());
        } else {
            calcDmgMax = player.calculateMaxBaseDamage(player.getTotalWatk());
        }

        Skill skill = SkillFactory.getSkill(ret.skill);

        if (ret.skill != 0) {
            player.applyHiddenSkillFixes(skill);
            MapleStatEffect effect = skill.getEffect(ret.skillLevel);

            if (magic) {
                // Since the skill is magic based, use the magic formula
                if (player.getJob() == MapleJob.IL_ARCHMAGE || player.getJob() == MapleJob.IL_MAGE) {
                    int skillLvl = player.getSkillLevel(ILMage.ELEMENT_AMPLIFICATION);
                    if (skillLvl > 0) {
                        calcDmgMax = calcDmgMax * SkillFactory.getSkill(ILMage.ELEMENT_AMPLIFICATION).getEffect(skillLvl).getY() / 100;
                    }
                } else if (player.getJob() == MapleJob.FP_ARCHMAGE || player.getJob() == MapleJob.FP_MAGE) {
                    int skillLvl = player.getSkillLevel(FPMage.ELEMENT_AMPLIFICATION);
                    if (skillLvl > 0) {
                        calcDmgMax = calcDmgMax * SkillFactory.getSkill(FPMage.ELEMENT_AMPLIFICATION).getEffect(skillLvl).getY() / 100;
                    }
                } else if (player.getJob() == MapleJob.BLAZEWIZARD3 || player.getJob() == MapleJob.BLAZEWIZARD4) {
                    int skillLvl = player.getSkillLevel(BlazeWizard.ELEMENT_AMPLIFICATION);
                    if (skillLvl > 0) {
                        calcDmgMax = calcDmgMax * SkillFactory.getSkill(BlazeWizard.ELEMENT_AMPLIFICATION).getEffect(skillLvl).getY() / 100;
                    }
                } else if (player.getJob() == MapleJob.EVAN7 || player.getJob() == MapleJob.EVAN8 || player.getJob() == MapleJob.EVAN9 || player.getJob() == MapleJob.EVAN10) {
                    int skillLvl = player.getSkillLevel(Evan.MAGIC_AMPLIFICATION);
                    if (skillLvl > 0) {
                        calcDmgMax = calcDmgMax * SkillFactory.getSkill(Evan.MAGIC_AMPLIFICATION).getEffect(skillLvl).getY() / 100;
                    }
                }

                calcDmgMax *= effect.getMatk();
                if (ret.skill == Cleric.HEAL) {
                    // This formula is still a bit wonky, but it is fairly accurate.
                    calcDmgMax = (int) Math.round((player.getTotalInt() * 4.8 + player.getTotalLuk() * 4) * player.getTotalMagic() / 1000);
                    calcDmgMax = calcDmgMax * effect.getHp() / 100;
                }
            } else if (ret.skill == Hermit.SHADOW_MESO) {
                // Shadow Meso also has its own formula
                calcDmgMax = effect.getMoneyCon() * 10;
                calcDmgMax = (int) Math.floor(calcDmgMax * 1.5);
            } else {
                // Normal damage formula for skills
                calcDmgMax = calcDmgMax * effect.getDamage() / 100;
            }
        }

        Integer comboBuff = player.getBuffedValue(MapleBuffStat.COMBO_COUNTER);
        if (comboBuff != null && comboBuff > 0) {
            int cSkillID = player.isCygnus() ? DawnWarrior.COMBO_ATTACK : Crusader.COMBO_ATTACK;
            int acSkillID = player.isCygnus() ? DawnWarrior.ADVANCED_COMBO : Hero.ADVANCED_COMBO_ATTACK;

            if (comboBuff > 6) {
                // Advanced Combo
                MapleStatEffect ceffect = SkillFactory.getSkill(acSkillID).getEffect(player);
                if (ceffect != null) {
                    calcDmgMax = (int) Math.floor(calcDmgMax * (ceffect.getDamage() + 50) / 100f + 0.20 + (comboBuff - 5) * 0.04);
                }
            } else {
                // Normal Combo
                byte skillLevel = player.getSkillLevel(cSkillID);
                MapleStatEffect effect = SkillFactory.getSkill(cSkillID).getEffect(player);
                if (skillLevel > 0 && effect != null) {
                    calcDmgMax = (int) Math.floor(calcDmgMax * (effect.getDamage() + 50) / 100f + Math.floor((comboBuff - 1) * (skillLevel / 6)) / 100);
                }
            }

            if (GameConstants.isFinisherSkill(ret.skill)) {
                // Finisher skills do more damage based on how many orbs the player has.
                int orbs = comboBuff - 1;
                if (orbs == 2) {
                    calcDmgMax *= 1.2;
                } else if (orbs == 3) {
                    calcDmgMax *= 1.54;
                } else if (orbs == 4) {
                    calcDmgMax *= 2;
                } else if (orbs >= 5) {
                    calcDmgMax *= 2.5;
                }
            }
        }

        if (player.getEnergyBar() == 15000) {
            int energycharge = player.isCygnus() ? ThunderBreaker.ENERGY_CHARGE : Marauder.ENERGY_CHARGE;
            MapleStatEffect ceffect = SkillFactory.getSkill(energycharge).getEffect(player.getSkillLevel(energycharge));
            calcDmgMax *= ceffect.getDamage() / 100;
        }

        if (player.getMapId() >= 914000000 && player.getMapId() <= 914000500) {
            calcDmgMax += 80000; // Aran Tutorial.
        }

        boolean canCrit = false;
        if (player.getJob().isA((MapleJob.BOWMAN)) || player.getJob().isA(MapleJob.THIEF) || player.getJob().isA(MapleJob.NIGHTWALKER1) || player.getJob().isA(MapleJob.WINDARCHER1) || player.getJob() == MapleJob.ARAN3 || player.getJob() == MapleJob.ARAN4 || player.getJob() == MapleJob.MARAUDER || player.getJob() == MapleJob.BUCCANEER) {
            canCrit = true;
        }
        if (player.getEffects().containsKey(MapleBuffStat.SHARP_EYES)) {
            // Any class that has sharp eyes can crit. Also, since it stacks with normal crit go ahead
            // and calc it in.
            canCrit = true;
            calcDmgMax *= 2;
        }

        boolean shadowPartner = false;
        if (player.getEffects().containsKey(MapleBuffStat.SHADOW_PARTNER)) {
            shadowPartner = true;
        }

        for (int i = 0; i < ret.numAttacked; i++) {
            int oid = reader.readInt();
            reader.skip(14);
            List<Integer> allDamageNumbers = new ArrayList<>();
            MapleMonster monster = player.getMap().getMonsterByOid(oid);

            BuffContainer container = player.getEffects().get(MapleBuffStat.WEAPON_CHARGE);
            if (container != null) {
                // Charge, so now we need to check elemental effectiveness
                int sourceID = container.getSourceID();
                int level = container.getValue();
                if (monster != null) {
                    if (sourceID == WhiteKnight.FLAME_CHARGE_BW || sourceID == WhiteKnight.FIRE_CHARGE_SWORD) {
                        if (monster.getStats().getEffectiveness(Element.FIRE) == ElementalEffectiveness.WEAK) {
                            calcDmgMax *= 1.05 + level * 0.015;
                        }
                    } else if (sourceID == WhiteKnight.BLIZZARD_CHARGE_BW || sourceID == WhiteKnight.ICE_CHARGE_SWORD) {
                        if (monster.getStats().getEffectiveness(Element.ICE) == ElementalEffectiveness.WEAK) {
                            calcDmgMax *= 1.05 + level * 0.015;
                        }
                    } else if (sourceID == WhiteKnight.LIGHTNING_CHARGE_BW || sourceID == WhiteKnight.THUNDER_CHARGE_SWORD) {
                        if (monster.getStats().getEffectiveness(Element.LIGHTING) == ElementalEffectiveness.WEAK) {
                            calcDmgMax *= 1.05 + level * 0.015;
                        }
                    } else if (sourceID == Paladin.DIVINE_CHARGE_BW || sourceID == Paladin.HOLY_CHARGE_SWORD) {
                        if (monster.getStats().getEffectiveness(Element.HOLY) == ElementalEffectiveness.WEAK) {
                            calcDmgMax *= 1.2 + level * 0.015;
                        }
                    }
                } else {
                    // Since we already know the skill has an elemental attribute, but we dont know if the monster is weak or not, lets
                    // take the safe approach and just assume they are weak.
                    calcDmgMax *= 1.5;
                }
            }

            if (ret.skill != 0) {
                if (skill.getElement() != Element.NEUTRAL && player.getBuffedValue(MapleBuffStat.ELEMENTAL_RESET) == null) {
                    // The skill has an element effect, so we need to factor that in.
                    if (monster != null) {
                        ElementalEffectiveness eff = monster.getEffectiveness(skill.getElement());
                        if (eff == ElementalEffectiveness.WEAK) {
                            calcDmgMax *= 1.5;
                        } else if (eff == ElementalEffectiveness.STRONG) {
                            //calcDmgMax *= 0.5;
                        }
                    } else {
                        // Since we already know the skill has an elemental attribute, but we dont know if the monster is weak or not, lets
                        // take the safe approach and just assume they are weak.
                        calcDmgMax *= 1.5;
                    }
                }
                if (ret.skill == Hermit.SHADOW_WEB) {
                    if (monster != null) {
                        calcDmgMax = monster.getHp() / (50 - player.getSkillLevel(skill));
                    }
                }
                MapleStatEffect effect = ret.getAttackEffect(player, skill);
                if (effect != null) {
                    int fixed = effect.getFixDamage();
                    if (fixed > 0) {
                        calcDmgMax = fixed;
                    }
                }
            }

            for (int j = 0; j < ret.numDamage; j++) {
                int damage = reader.readInt() & 0x7FFFFFFF;
                long hitDmgMax = calcDmgMax;
                if (ret.skill == Buccaneer.BARRAGE) {
                    if (j > 3) {
                        hitDmgMax *= Math.pow(2, (j - 3));
                    }
                }
                if (shadowPartner) {
                    // For shadow partner, the second half of the hits only do 50% damage. So calc that in for the crit effects.
                    if (j >= ret.numDamage / 2) {
                        hitDmgMax *= 0.5;
                    }
                }

                if (ret.skill == Marksman.SNIPE) {
                    damage = 195000 + Randomizer.nextInt(5000);
                    hitDmgMax = 200000;
                }

                if (ret.skill == Marksman.SNIPE || (canCrit && damage > hitDmgMax)) {
                    damage = (-Integer.MAX_VALUE + damage - 1) & 0x7FFFFFFF;
                }

                allDamageNumbers.add(damage);
            }
            if (ret.skill != Corsair.RAPID_FIRE
                    || ret.skill != Aran.HIDDEN_FULL_SWING_DOUBLE_SWING
                    || ret.skill != Aran.HIDDEN_FULL_SWING_TRIPLE_SWING
                    || ret.skill != Aran.HIDDEN_OVER_SWING_DOUBLE_SWING
                    || ret.skill != Aran.HIDDEN_OVER_SWING_TRIPLE_SWING) {
                reader.skip(4);
            }
            ret.allDamage.put(oid, allDamageNumbers);
        }
        if (ret.skill == NightWalker.POISON_BOMB) { // Poison Bomb
            reader.skip(4);
            ret.position.setLocation(reader.readShort(), reader.readShort());
        }
        return ret;
    }

    void addCooldown(AttackInfo attackInfo) {
        MapleCharacter player = getClient().getPlayer();
        if (attackInfo.skill > 0) {
            Skill skill = SkillFactory.getSkill(attackInfo.skill);
            MapleStatEffect effect_ = skill.getEffect(player.getSkillLevel(skill));
            if (effect_.getCooldown() > 0) {
                if (!player.skillisCooling(attackInfo.skill)) {
                    getClient().announce(MaplePacketCreator.skillCooldown(attackInfo.skill, effect_.getCooldown()));
                    player.addCooldown(attackInfo.skill, System.currentTimeMillis(), effect_.getCooldown() * 1000, TaskExecutor.createTask(new MapleCharacter.CancelCooldownAction(player, attackInfo.skill), effect_.getCooldown() * 1000));
                }
            }
        }
    }

    public boolean cantUseSkill(){

        if(getClient().getPlayer().isGM()){
            return false;
        }

        int current_map = getClient().getPlayer().getMapId();
        int counter;
        for(counter = 0; counter < ServerConstants.NO_SKILL_MAPS.length; counter++) {
            if (current_map == ServerConstants.NO_SKILL_MAPS[counter]) {
                return true;
            }
        }
        return false;
    }
    public static class AttackInfo {

        public int numAttacked, numDamage, numAttackedAndDamage, skill, skillLevel, stance, direction, rangeDirection, charge, display;
        public Map<Integer, List<Integer>> allDamage;
        public boolean isHH = false, isTempest = false, ranged, magic;
        public int speed = 4;
        public Point position = new Point();

        @Override
        public String toString() {
            return String.format("AttackInfo{skill=%d, skillLevel=%d}", skill, skillLevel);
        }

        public MapleStatEffect getAttackEffect(MapleCharacter chr, Skill theSkill) {
            Skill mySkill = theSkill;
            if (mySkill == null) {
                mySkill = SkillFactory.getSkill(GameConstants.getHiddenSkill(skill));
            }
            int skillLevel = chr.getSkillLevel(mySkill);
            if (mySkill.getId() % 10000000 == 1020) {
                if (chr.getPartyQuest() instanceof Pyramid) {
                    if (((Pyramid) chr.getPartyQuest()).useSkill()) {
                        skillLevel = 1;
                    }
                }
            }
            if (skillLevel == 0) {
                return null;
            }
            if (display > 80) {
                if (!theSkill.getAction()) {
                    return null;
                }
            }
            return mySkill.getEffect(skillLevel);
        }
    }
}
