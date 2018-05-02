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

import client.MapleBuffStat;
import client.MapleCharacter;
import client.Skill;
import client.SkillFactory;
import client.autoban.Cheater;
import client.autoban.Cheats;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.meta.Occupation;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import constants.skills.Aran;
import constants.skills.Corsair;
import net.PacketEvent;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MapleStatEffect;
import server.life.*;
import server.life.MapleLifeFactory.LoseItem;
import server.maps.MapleMap;
import tools.MaplePacketCreator;
import tools.Randomizer;
import tools.data.input.SeekableLittleEndianAccessor;

import java.awt.*;
import java.util.Collections;
import java.util.List;

public final class TakeDamageEvent extends PacketEvent {

    private int damage;
    private int monsterIdFrom = 0;
    private int objectId = 0;

    private byte damageFrom;
    private byte direction = 0;

    @Override
    public void process(SeekableLittleEndianAccessor slea) {
        slea.skip(4);
        damageFrom = slea.readByte();
        slea.skip(1); // Element
        damage = slea.readInt();
        if (damageFrom != -3 && damageFrom != -4) {
            monsterIdFrom = slea.readInt();
            objectId = slea.readInt();
            direction = slea.readByte();
        }
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();

        if (player.isImmortal()) {
            if (player.getHp() - damage < 1) {
                damage = (player.getHp() - 1);
            }
        }

        int pgmr = 0;
        int pos_x = 0, pos_y = 0, fake = 0;
        boolean is_pgmr = false, is_pg = true;
        int mpattack = 0;
        MapleMonster attacker = null;
        final MapleMap map = player.getMap();
        if (damageFrom != -3 && damageFrom != -4) {
            attacker = (MapleMonster) map.getMapObject(objectId);
            List<LoseItem> loseItems;
            if (attacker != null) {
                if (attacker.isBuffed(MonsterStatus.NEUTRALISE)) {
                    if (player.getArcade() != null) {
                        player.getArcade().onHit(attacker.getId());
                    }
                    return null;
                }
                if (damage > 0) {
                    loseItems = map.getMonsterById(monsterIdFrom).getStats().loseItem();
                    if (loseItems != null) {
                        MapleInventoryType type;
                        final int xPosition = player.getPosition().x;
                        byte d = 1;
                        Point pos = new Point(0, player.getPosition().y);
                        for (LoseItem loseItem : loseItems) {
                            type = MapleItemInformationProvider.getInstance().getInventoryType(loseItem.getId());
                            for (byte b = 0; b < loseItem.getX(); b++) {//LOL?
                                if (Randomizer.nextInt(101) >= loseItem.getChance()) {
                                    if (player.haveItem(loseItem.getId())) {
                                        pos.x = xPosition + ((d % 2 == 0) ? (25 * (d + 1) / 2) : -(25 * (d / 2)));
                                        MapleInventoryManipulator.removeById(getClient(), type, loseItem.getId(), 1, false, false);
                                        map.spawnItemDrop(player, player, new Item(loseItem.getId(), (short) 0, (short) 1), map.calcDropPos(pos, player.getPosition()), true, true);
                                        d++;
                                    } else {
                                        break;
                                    }
                                }
                            }
                        }
                        map.removeMapObject(attacker);
                    }
                }
            } else {
                return null;
            }
        }
        if (damageFrom != -1 && damageFrom != -2 && attacker != null) {
            MobAttackInfo attackInfo = MobAttackInfoFactory.getMobAttackInfo(attacker, damageFrom);
            if (attackInfo != null) {
                if (attackInfo.isDeadlyAttack()) {
                    mpattack = player.getMp() - 1;
                }
                mpattack += attackInfo.getMpBurn();
                MobSkill skill = MobSkillFactory.getMobSkill(attackInfo.getDiseaseSkill(), attackInfo.getDiseaseLevel());
                if (player.getOccupation() == null || player.getOccupation().getType() != Occupation.Type.Demon) {
                    if (skill != null && damage > 0) {
                        skill.applyEffect(player, attacker, false);
                    }
                }

                attacker.setMp(attacker.getMp() - attackInfo.getMpCon());
                if (player.getBuffedValue(MapleBuffStat.MANA_REFLECTION) != null && damage > 0 && !attacker.isBoss()) {
                    int jobid = player.getJob().getId();
                    if (jobid == 212 || jobid == 222 || jobid == 232) {
                        int id = jobid * 10000 + 1002;
                        Skill manaReflectSkill = SkillFactory.getSkill(id);
                        if (player.isBuffFrom(MapleBuffStat.MANA_REFLECTION, manaReflectSkill) && player.getSkillLevel(manaReflectSkill) > 0 && manaReflectSkill.getEffect(player.getSkillLevel(manaReflectSkill)).makeChanceResult()) {
                            int bouncedamage = (damage * manaReflectSkill.getEffect(player.getSkillLevel(manaReflectSkill)).getX() / 100);
                            if (bouncedamage > attacker.getMaxHp() / 5) {
                                bouncedamage = attacker.getMaxHp() / 5;
                            }
                            map.damageMonster(player, attacker, bouncedamage);
                            map.broadcastMessage(player, MaplePacketCreator.damageMonster(objectId, bouncedamage), true);
                            player.getClient().announce(MaplePacketCreator.showOwnBuffEffect(id, 5));
                            map.broadcastMessage(player, MaplePacketCreator.showBuffeffect(player.getId(), id, 5), false);
                        }
                    }
                }
            }
        }
        if (damage == -1) {
            fake = 4020002 + (player.getJob().getId() / 10 - 40) * 100000;
        }

        Cheater.CheatEntry entry = player.getCheater().getCheatEntry(Cheats.GodMode);
        if (damage == 0 && !player.isGM()) {
            entry.spamCount++;
            if (entry.spamCount % 15 == 0) {
                entry.incrementCheatCount();
                entry.announce(player.getClient(), String.format("[%d] %s has %d consecutive misses (possible god mode)", entry.cheatCount, player.getName(), entry.spamCount), 10000);
            }
            entry.latestOperationTimestamp = System.currentTimeMillis();
        } else {
            entry.spamCount = 0;
        }
        if (damage > 0 && !player.isHidden()) {
            if (attacker != null && damageFrom == -1 && player.getBuffedValue(MapleBuffStat.POWERGUARD) != null) { // PG works on bosses, but only at half of the rate.
                int bouncedamage = (int) (damage * (player.getBuffedValue(MapleBuffStat.POWERGUARD).doubleValue() / (attacker.isBoss() ? 200 : 100)));
                bouncedamage = Math.min(bouncedamage, attacker.getMaxHp() / 10);
                damage -= bouncedamage;
                map.damageMonster(player, attacker, bouncedamage);
                map.broadcastMessage(player, MaplePacketCreator.damageMonster(objectId, bouncedamage), false, true);
                player.checkMonsterAggro(attacker);
            }
            if (attacker != null && damageFrom == -1 && player.getBuffedValue(MapleBuffStat.BODY_PRESSURE) != null) {
                Skill skill = SkillFactory.getSkill(Aran.BODY_PRESSURE);
                final MapleStatEffect eff = skill.getEffect(player.getSkillLevel(skill));
                if (!attacker.alreadyBuffedStats().contains(MonsterStatus.NEUTRALISE)) {
                    if (!attacker.isBoss() && eff.makeChanceResult()) {
                        attacker.applyStatus(player, new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.NEUTRALISE, 1), skill, null, false), false, (eff.getDuration() / 10) * 2, false);
                    }
                }
            }
            if (damageFrom != -3 && damageFrom != -4) {
                int achilles = 0;
                Skill achilles1 = null;
                int jobid = player.getJob().getId();
                if (jobid < 200 && jobid % 10 == 2) {
                    achilles1 = SkillFactory.getSkill(jobid * 10000 + (jobid == 112 ? 4 : 5));
                    achilles = player.getSkillLevel(achilles1);
                }
                if (achilles != 0 && achilles1 != null) {
                    damage *= (achilles1.getEffect(achilles).getX() / 1000.0);
                }
            }
            Integer mesoguard = player.getBuffedValue(MapleBuffStat.MESOGUARD);
            if (player.getBuffedValue(MapleBuffStat.MAGIC_GUARD) != null && mpattack == 0) {
                int mploss = (int) (damage * (player.getBuffedValue(MapleBuffStat.MAGIC_GUARD).doubleValue() / 100.0));
                int hploss = damage - mploss;
                if (mploss > player.getMp()) {
                    hploss += mploss - player.getMp();
                    mploss = player.getMp();
                }
                player.addMPHP(-hploss, -mploss);
            } else if (mesoguard != null) {
                damage = Math.round(damage / 2);
                int mesoloss = (int) (damage * (mesoguard.doubleValue() / 100.0));
                if (player.getMeso() < mesoloss) {
                    player.gainMeso(-player.getMeso(), false);
                    player.cancelBuffStats(MapleBuffStat.MESOGUARD);
                } else {
                    player.gainMeso(-mesoloss, false);
                }
                player.addMPHP(-damage, -mpattack);
            } else {
                if (player.getBuffedValue(MapleBuffStat.MONSTER_RIDING) != null) {
                    if (player.getBuffedValue(MapleBuffStat.MONSTER_RIDING) == Corsair.BATTLESHIP) {
                        player.decreaseBattleshipHp(damage);
                    }
                }
                player.addMPHP(-damage, -mpattack);
            }
        }
        if (!player.isHidden()) {
            map.broadcastMessage(player, MaplePacketCreator.damagePlayer(damageFrom, monsterIdFrom, player.getId(), damage, fake, direction, is_pgmr, pgmr, is_pg, objectId, pos_x, pos_y), false);
            FakePlayer fakePlayer = player.getFakePlayer();
            if (fakePlayer != null && fakePlayer.isFollowing()) {
                map.broadcastMessage(fakePlayer, MaplePacketCreator.damagePlayer(damageFrom, monsterIdFrom, fakePlayer.getId(), damage, fake, direction, is_pgmr, pgmr, is_pg, objectId, pos_x, pos_y), false);
            }
            player.checkBerserk();
        }

        if (player.getArcade() != null && attacker != null) {
            player.getArcade().onHit(attacker.getId());
        }

        if (map.getId() >= 925020000 && map.getId() < 925030000) {
            player.setDojoEnergy(player.isGM() ? 300 : player.getDojoEnergy() < 300 ? player.getDojoEnergy() + 1 : 0); //Fking gm's
            player.getClient().announce(MaplePacketCreator.getEnergy("energy", player.getDojoEnergy()));
        }
        return null;
    }

    public int getDamage() {
        return damage;
    }

    public void setDamage(int damage) {
        this.damage = damage;
    }

    public int getMonsterIdFrom() {
        return monsterIdFrom;
    }

    public int getObjectId() {
        return objectId;
    }

    public byte getDamageFrom() {
        return damageFrom;
    }

    public byte getDirection() {
        return direction;
    }
}
