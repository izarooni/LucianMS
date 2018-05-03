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
package com.lucianms.server.events.channel;

import client.*;
import client.autoban.Cheater;
import client.autoban.Cheats;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.meta.Occupation;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import constants.GameConstants;
import constants.ItemConstants;
import constants.skills.*;
import net.PacketEvent;
import com.lucianms.scheduler.TaskExecutor;
import server.MapleItemInformationProvider;
import server.MapleStatEffect;
import server.life.*;
import server.maps.MapleMap;
import server.maps.MapleMapItem;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.partyquest.Pyramid;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.Randomizer;
import tools.data.input.LittleEndianAccessor;

import java.awt.*;
import java.util.*;
import java.util.List;

public abstract class AbstractDealDamageEvent extends PacketEvent {

    public static class AttackInfo {

        public int numAttacked, numDamage, numAttackedAndDamage, skill, skilllevel, stance, direction, rangedirection, charge, display;
        public Map<Integer, List<Integer>> allDamage;
        public boolean isHH = false, isTempest = false, ranged, magic;
        public int speed = 4;
        public Point position = new Point();

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
            if (display > 80) { //Hmm
                if (!theSkill.getAction()) {
                    return null;
                }
            }
            return mySkill.getEffect(skillLevel);
        }
    }

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

                if (player.getMp() < attackEffect.getMpCon()) {
                    Cheater.CheatEntry entry = player.getCheater().getCheatEntry(Cheats.InsufficientMana);
                    entry.incrementCheatCount();
                    entry.announce(player.getClient(), String.format("[%d] %s insufficient MP to use skill %d", entry.cheatCount, player.getName(), attack.skill), 5000);
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
                int mobCount = attackEffect.getMobCount();
                if (attack.skill == DawnWarrior.FINAL_ATTACK || attack.skill == Page.FINAL_ATTACK_BW || attack.skill == Page.FINAL_ATTACK_SWORD || attack.skill == Fighter.FINAL_ATTACK_SWORD || attack.skill == Fighter.FINAL_ATTACK_AXE || attack.skill == Spearman.FINAL_ATTACK_SPEAR || attack.skill == Spearman.FINAL_ATTACK_POLE_ARM || attack.skill == WindArcher.FINAL_ATTACK || attack.skill == DawnWarrior.FINAL_ATTACK || attack.skill == Hunter.FINAL_ATTACK_BOW || attack.skill == Crossbowman.FINAL_ATTACK) {
                    mobCount = 15;//:(
                }

                if (attack.skill == Aran.HIDDEN_FULL_SWING_DOUBLE || attack.skill == Aran.HIDDEN_FULL_SWING_TRIPLE || attack.skill == Aran.HIDDEN_OVER_SWING_DOUBLE || attack.skill == Aran.HIDDEN_OVER_SWING_TRIPLE) {
                    mobCount = 12;
                }

                if (attack.numAttacked > mobCount) {
                    Cheater.CheatEntry entry = player.getCheater().getCheatEntry(Cheats.ConcurrentAttacks);
                    entry.incrementCheatCount();
                    entry.announce(player.getClient(), String.format("[%d] %s attacking too many monsters at once (%d monsters, should be %d)", entry.cheatCount, player.getName(), attack.numAttacked, attackEffect.getMobCount()), 5000);
                }
            }
            if (!player.isAlive()) {
                return;
            }

            int totDamage = 0;
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
            for (Integer oned : attack.allDamage.keySet()) {
                final MapleMonster monster = map.getMonsterByOid(oned);
                if (monster != null) {
                    double distance = player.getPosition().distanceSq(monster.getPosition());
                    double distanceToDetect = 200000.0;

                    if (attack.ranged) {
                        distanceToDetect += 400000;
                    }

                    if (attack.magic) {
                        distanceToDetect += 200000;
                    }

                    if (player.getJob().isA(MapleJob.ARAN1)) {
                        distanceToDetect += 200000; // Arans have extra range over normal warriors.
                    }

                    if (attack.skill == Aran.COMBO_SMASH || attack.skill == Aran.BODY_PRESSURE) {
                        distanceToDetect += 40000;
                    }

                    if (attack.skill == Bishop.GENESIS || attack.skill == ILArchMage.BLIZZARD || attack.skill == FPArchMage.METEOR_SHOWER) {
                        distanceToDetect += 275000;
                    }

                    if (attack.skill == Hero.BRANDISH || attack.skill == DragonKnight.SPEAR_CRUSHER || attack.skill == DragonKnight.POLE_ARM_CRUSHER) {
                        distanceToDetect += 40000;
                    }

                    if (attack.skill == DragonKnight.DRAGON_ROAR || attack.skill == SuperGM.SUPER_DRAGON_ROAR) {
                        distanceToDetect += 250000;
                    }

                    if (attack.skill == Shadower.BOOMERANG_STEP) {
                        distanceToDetect += 60000;
                    }

                    int totDamageToOneMonster = 0;
                    List<Integer> onedList = attack.allDamage.get(oned);
                    for (Integer eachd : onedList) {
                        if (eachd < 0) {
                            eachd += Integer.MAX_VALUE;
                        }
                        totDamageToOneMonster += eachd;
                    }
                    totDamage += totDamageToOneMonster;
                    player.checkMonsterAggro(monster);
                    if (player.getBuffedValue(MapleBuffStat.PICKPOCKET) != null && (attack.skill == 0 || attack.skill == Rogue.DOUBLE_STAB || attack.skill == Bandit.SAVAGE_BLOW || attack.skill == ChiefBandit.ASSAULTER || attack.skill == ChiefBandit.BAND_OF_THIEVES || attack.skill == Shadower.ASSASSINATE || attack.skill == Shadower.TAUNT || attack.skill == Shadower.BOOMERANG_STEP)) {
                        Skill pickpocket = SkillFactory.getSkill(ChiefBandit.PICKPOCKET);
                        int delay = 0;
                        final int maxmeso = player.getBuffedValue(MapleBuffStat.PICKPOCKET);
                        for (Integer eachd : onedList) {

                            eachd += Integer.MAX_VALUE;
                            if (pickpocket.getEffect(player.getSkillLevel(pickpocket)).makeChanceResult()) {
                                final Integer eachdf;
                                if (eachd < 0) {
                                    eachdf = eachd + Integer.MAX_VALUE;
                                } else {
                                    eachdf = eachd;
                                }

                                TaskExecutor.createTask(() -> player.getMap().spawnMesoDrop(Math.min((int) Math.max(((double) eachdf / (double) 20000) * (double) maxmeso, (double) 1), maxmeso), new Point((int) (monster.getPosition().getX() + Randomizer.nextInt(100) - 50), (int) (monster.getPosition().getY())), monster, player, true, (byte) 2), delay);
                                delay += 100;
                            }
                        }
                    } else if (attack.skill == Marauder.ENERGY_DRAIN || attack.skill == ThunderBreaker.ENERGY_DRAIN || attack.skill == NightWalker.VAMPIRE || attack.skill == Assassin.DRAIN) {
                        player.addHP(Math.min(monster.getMaxHp(), Math.min((int) ((double) totDamage * (double) SkillFactory.getSkill(attack.skill).getEffect(player.getSkillLevel(SkillFactory.getSkill(attack.skill))).getX() / 100.0), player.getMaxHp() / 2)));
                    } else if (attack.skill == Bandit.STEAL) {
                        Skill steal = SkillFactory.getSkill(Bandit.STEAL);
                        if (monster.getStolen().size() < 1) { // One steal per mob <3
                            if (Math.random() < 0.3 && steal.getEffect(player.getSkillLevel(steal)).makeChanceResult()) { //Else it drops too many cool stuff :(
                                List<MonsterDropEntry> toSteals = MapleMonsterInformationProvider.getInstance().retrieveDrop(monster.getId());
                                Collections.shuffle(toSteals);
                                int toSteal = toSteals.get(Randomizer.rand(0, (toSteals.size() - 1))).itemId;
                                MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                                Item item;
                                if (ItemConstants.getInventoryType(toSteal).equals(MapleInventoryType.EQUIP)) {
                                    item = ii.randomizeStats((Equip) ii.getEquipById(toSteal));
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
                        player.announce(MaplePacketCreator.giveBuff(1, attack.skill, Collections.singletonList(new Pair<>(MapleBuffStat.HOMING_BEACON, monster.getObjectId()))));
                    }

                    if (job == 2111 || job == 2112) {
                        if (player.getBuffedValue(MapleBuffStat.WK_CHARGE) != null) {
                            Skill snowCharge = SkillFactory.getSkill(Aran.SNOW_CHARGE);
                            if (totDamageToOneMonster > 0) {
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
                            if (player.isBuffFrom(MapleBuffStat.WK_CHARGE, chargeSkill)) {
                                if (totDamageToOneMonster > 0) {
                                    monster.setTempEffectiveness(Element.ICE, ElementalEffectiveness.WEAK, chargeSkill.getEffect(player.getSkillLevel(chargeSkill)).getY() * 1000);
                                    break;
                                }
                            }
                        }
                        if (job == 122) {
                            for (int charge = 1221003; charge < 1221004; charge++) {
                                Skill chargeSkill = SkillFactory.getSkill(charge);
                                if (player.isBuffFrom(MapleBuffStat.WK_CHARGE, chargeSkill)) {
                                    if (totDamageToOneMonster > 0) {
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
                            player.setHp(player.getHp() + ((totDamage * skill.getEffect(player.getSkillLevel(skill)).getX()) / 100), true);
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
                    if (totDamageToOneMonster > 0 && attackEffect != null && attackEffect.getMonsterStati().size() > 0) {
                        if (attackEffect.makeChanceResult()) {
                            monster.applyStatus(player, new MonsterStatusEffect(attackEffect.getMonsterStati(), theSkill, null, false), attackEffect.isPoison(), attackEffect.getDuration());
                        }
                    }
                    if (attack.isHH && !monster.isBoss()) {
                        map.damageMonster(player, monster, monster.getHp() - 1);
                    } else if (attack.isHH) {
                        int HHDmg = (player.calculateMaxBaseDamage(player.getTotalWatk()) * (SkillFactory.getSkill(Paladin.HEAVENS_HAMMER).getEffect(player.getSkillLevel(SkillFactory.getSkill(Paladin.HEAVENS_HAMMER))).getDamage() / 100));
                        map.damageMonster(player, monster, (int) (Math.floor(Math.random() * (HHDmg / 5) + HHDmg * .8)));
                    } else if (attack.isTempest && !monster.isBoss()) {
                        map.damageMonster(player, monster, monster.getHp());
                    } else if (attack.isTempest) {
                        int TmpDmg = (player.calculateMaxBaseDamage(player.getTotalWatk()) * (SkillFactory.getSkill(Aran.COMBO_TEMPEST).getEffect(player.getSkillLevel(SkillFactory.getSkill(Aran.COMBO_TEMPEST))).getDamage() / 100));
                        map.damageMonster(player, monster, (int) (Math.floor(Math.random() * (TmpDmg / 5) + TmpDmg * .8)));
                    } else {
                        map.damageMonster(player, monster, totDamageToOneMonster);
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
                    Equip weapon = (Equip) player.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
                    if (weapon != null && weapon.isRegalia() && !monster.isDamagedOvertime()) {
                        monster.applyDamageOvertime(player, 5 * 1000);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    AttackInfo parseDamage(LittleEndianAccessor lea, boolean ranged, boolean magic) {
        MapleCharacter player = getClient().getPlayer();
        //2C 00 00 01 91 A1 12 00 A5 57 62 FC E2 75 99 10 00 47 80 01 04 01 C6 CC 02 DD FF 5F 00
        AttackInfo ret = new AttackInfo();
        lea.readByte();
        ret.numAttackedAndDamage = lea.readByte();
        ret.numAttacked = (ret.numAttackedAndDamage >>> 4) & 0xF;
        ret.numDamage = ret.numAttackedAndDamage & 0xF;
        ret.allDamage = new HashMap<>();
        ret.skill = lea.readInt();
        ret.ranged = ranged;
        ret.magic = magic;
        if (ret.skill > 0) {
            ret.skilllevel = player.getSkillLevel(ret.skill);
        }
        if (ret.skill == Evan.ICE_BREATH || ret.skill == Evan.FIRE_BREATH || ret.skill == FPArchMage.BIG_BANG || ret.skill == ILArchMage.BIG_BANG || ret.skill == Bishop.BIG_BANG || ret.skill == Gunslinger.GRENADE || ret.skill == Brawler.CORKSCREW_BLOW || ret.skill == ThunderBreaker.CORKSCREW_BLOW || ret.skill == NightWalker.POISON_BOMB) {
            ret.charge = lea.readInt();
        } else {
            ret.charge = 0;
        }
        if (ret.skill == Paladin.HEAVENS_HAMMER) {
            ret.isHH = true;
        } else if (ret.skill == Aran.COMBO_TEMPEST) {
            ret.isTempest = true;
        }
        lea.skip(8);
        ret.display = lea.readByte();
        ret.direction = lea.readByte();
        ret.stance = lea.readByte();
        if (ret.skill == ChiefBandit.MESO_EXPLOSION) {
            if (ret.numAttackedAndDamage == 0) {
                lea.skip(10);
                int bullets = lea.readByte();
                for (int j = 0; j < bullets; j++) {
                    int mesoid = lea.readInt();
                    lea.skip(1);
                    ret.allDamage.put(mesoid, null);
                }
                return ret;
            } else {
                lea.skip(6);
            }
            for (int i = 0; i < ret.numAttacked + 1; i++) {
                int oid = lea.readInt();
                if (i < ret.numAttacked) {
                    lea.skip(12);
                    int bullets = lea.readByte();
                    List<Integer> allDamageNumbers = new ArrayList<>();
                    for (int j = 0; j < bullets; j++) {
                        int damage = lea.readInt();
                        allDamageNumbers.add(damage);
                    }
                    ret.allDamage.put(oid, allDamageNumbers);
                    lea.skip(4);
                } else {
                    int bullets = lea.readByte();
                    for (int j = 0; j < bullets; j++) {
                        int mesoid = lea.readInt();
                        lea.skip(1);
                        ret.allDamage.put(mesoid, null);
                    }
                }
            }
            return ret;
        }
        if (ranged) {
            lea.readByte();
            ret.speed = lea.readByte();
            lea.readByte();
            ret.rangedirection = lea.readByte();
            lea.skip(7);
            if (ret.skill == Bowmaster.HURRICANE || ret.skill == Marksman.PIERCING_ARROW || ret.skill == Corsair.RAPID_FIRE || ret.skill == WindArcher.HURRICANE) {
                lea.skip(4);
            }
        } else {
            lea.readByte();
            ret.speed = lea.readByte();
            lea.skip(4);
        }
        int calcDmgMax = 0;

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

        if (ret.skill != 0) {
            Skill skill = SkillFactory.getSkill(ret.skill);
            MapleStatEffect effect = skill.getEffect(ret.skilllevel);

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

        Integer comboBuff = player.getBuffedValue(MapleBuffStat.COMBO);
        if (comboBuff != null && comboBuff > 0) {
            int oid = player.isCygnus() ? DawnWarrior.COMBO_ATTACK : Crusader.COMBO_ATTACK;
            int advcomboid = player.isCygnus() ? DawnWarrior.ADVANCED_COMBO : Hero.ADVANCED_COMBO_ATTACK;

            if (comboBuff > 6) {
                // Advanced Combo
                MapleStatEffect ceffect = SkillFactory.getSkill(advcomboid).getEffect(player.getSkillLevel(advcomboid));
                calcDmgMax = (int) Math.floor(calcDmgMax * (ceffect.getDamage() + 50) / 100 + 0.20 + (comboBuff - 5) * 0.04);
            } else {
                // Normal Combo
                MapleStatEffect ceffect = SkillFactory.getSkill(oid).getEffect(player.getSkillLevel(oid));
                calcDmgMax = (int) Math.floor(calcDmgMax * (ceffect.getDamage() + 50) / 100 + Math.floor((comboBuff - 1) * (player.getSkillLevel(oid) / 6)) / 100);
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
        if (player.getBuffEffect(MapleBuffStat.SHARP_EYES) != null) {
            // Any class that has sharp eyes can crit. Also, since it stacks with normal crit go ahead
            // and calc it in.
            canCrit = true;
            calcDmgMax *= 1.4;
        }

        boolean shadowPartner = false;
        if (player.getBuffEffect(MapleBuffStat.SHADOWPARTNER) != null) {
            shadowPartner = true;
        }

        if (ret.skill != 0) {
            int fixed = ret.getAttackEffect(player, SkillFactory.getSkill(ret.skill)).getFixDamage();
            if (fixed > 0) {
                calcDmgMax = fixed;
            }
        }
        for (int i = 0; i < ret.numAttacked; i++) {
            int oid = lea.readInt();
            lea.skip(14);
            List<Integer> allDamageNumbers = new ArrayList<>();
            MapleMonster monster = player.getMap().getMonsterByOid(oid);

            if (player.getBuffEffect(MapleBuffStat.WK_CHARGE) != null) {
                // Charge, so now we need to check elemental effectiveness
                int sourceID = player.getBuffSource(MapleBuffStat.WK_CHARGE);
                int level = player.getBuffedValue(MapleBuffStat.WK_CHARGE);
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
                Skill skill = SkillFactory.getSkill(ret.skill);
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
                if (ret.skill == FPWizard.POISON_BREATH || ret.skill == FPMage.POISON_MIST || ret.skill == FPArchMage.FIRE_DEMON || ret.skill == ILArchMage.ICE_DEMON) {
                    if (monster != null) {
                        // Turns out poison is completely server side, so I don't know why I added this. >.<
                        //calcDmgMax = monster.getHp() / (70 - chr.getSkillLevel(skill));
                    }
                } else if (ret.skill == Hermit.SHADOW_WEB) {
                    if (monster != null) {
                        calcDmgMax = monster.getHp() / (50 - player.getSkillLevel(skill));
                    }
                }
            }

            for (int j = 0; j < ret.numDamage; j++) {
                int damage = lea.readInt();
                int hitDmgMax = calcDmgMax;
                if (ret.skill == Buccaneer.BARRAGE) {
                    if (j > 3) {
                        hitDmgMax *= Math.pow(2, (j - 3));
                    }
                }
                if (shadowPartner) {
                    // For shadow partner, the second half of the hits only do 50% damage. So calc that
                    // in for the crit effects.
                    if (j >= ret.numDamage / 2) {
                        hitDmgMax *= 0.5;
                    }
                }

                if (ret.skill == Marksman.SNIPE) {
                    damage = 195000 + Randomizer.nextInt(5000);
                    hitDmgMax = 200000;
                }

                int maxWithCrit = hitDmgMax;
                if (canCrit) // They can crit, so up the max.
                {
                    maxWithCrit *= 2;
                }

                // Warn if the damage is over 1.5x what we calculated above.
                //if(damage > maxWithCrit * 1.5) {
                //  AutobanFactory.DAMAGE_HACK.alert(chr, "DMG: " + damage + " MaxDMG: " + maxWithCrit + " SID: " + ret.skill + " MobID: " + (monster != null ? monster.getId() : "null") + " Map: " + chr.getMap().getMapName() + " (" + chr.getMapId() + ")");
                //}

                // Add a ab point if its over 5x what we calculated.
                //	if(damage > maxWithCrit  * 5) {
                //	AutobanFactory.DAMAGE_HACK.addPoint(chr.getAutobanManager(), "DMG: " + damage + " MaxDMG: " + maxWithCrit + " SID: " + ret.skill + " MobID: " + (monster != null ? monster.getId() : "null") + " Map: " + chr.getMap().getMapName() + " (" + chr.getMapId() + ")");
                //}

                if (ret.skill == Marksman.SNIPE || (canCrit && damage > hitDmgMax)) {
                    // If the skill is a crit, inverse the damage to make it show up on clients.
                    damage = -Integer.MAX_VALUE + damage - 1;
                }

                allDamageNumbers.add(damage);
            }
            if (ret.skill != Corsair.RAPID_FIRE || ret.skill != Aran.HIDDEN_FULL_SWING_DOUBLE || ret.skill != Aran.HIDDEN_FULL_SWING_TRIPLE || ret.skill != Aran.HIDDEN_OVER_SWING_DOUBLE || ret.skill != Aran.HIDDEN_OVER_SWING_TRIPLE) {
                lea.skip(4);
            }
            ret.allDamage.put(oid, allDamageNumbers);
        }
        if (ret.skill == NightWalker.POISON_BOMB) { // Poison Bomb
            lea.skip(4);
            ret.position.setLocation(lea.readShort(), lea.readShort());
        }
        return ret;
    }

    @Override
    public void post() {
        MapleCharacter player = getClient().getPlayer();
        Occupation occupation = player.getOccupation();
        if (occupation != null && occupation.getType() == Occupation.Type.Undead) {
            int gain = player.getMaxHp() * ((int) Math.floor(Math.random() * 0.3 + 0.1));
            player.addHP(gain);
        }
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
}