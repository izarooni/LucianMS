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
import com.lucianms.client.Skill;
import com.lucianms.client.SkillFactory;
import com.lucianms.client.autoban.Cheater;
import com.lucianms.client.autoban.Cheats;
import com.lucianms.client.inventory.Item;
import com.lucianms.client.inventory.MapleInventoryType;
import com.lucianms.client.status.MonsterStatus;
import com.lucianms.client.status.MonsterStatusEffect;
import com.lucianms.constants.skills.Aran;
import com.lucianms.constants.skills.Corsair;
import com.lucianms.lang.GProperties;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.MapleInventoryManipulator;
import com.lucianms.server.MapleItemInformationProvider;
import com.lucianms.server.MapleStatEffect;
import com.lucianms.server.life.*;
import com.lucianms.server.life.MapleLifeFactory.LoseItem;
import com.lucianms.server.maps.MapleMap;
import com.lucianms.server.maps.MapleMapObject;
import tools.MaplePacketCreator;
import tools.Randomizer;

import java.awt.*;
import java.util.Collections;
import java.util.List;

public final class PlayerTakeDamageEvent extends PacketEvent {

    private int damage;
    private int monsterIdFrom = 0;
    private int objectId = 0;

    private byte damageFrom;
    private byte direction = 0;

    @Override
    public void processInput(MaplePacketReader reader) {
        reader.skip(4);
        damageFrom = reader.readByte();
        reader.skip(1); // Element
        damage = reader.readInt();
        if (damageFrom != -3 && damageFrom != -4) {
            monsterIdFrom = reader.readInt();
            objectId = reader.readInt();
            direction = reader.readByte();
        }
    }

    @Override
    public void packetCompleted() {
        MapleCharacter player = getClient().getPlayer();
        GProperties<Boolean> akm = player.getMap().getAutoKillMobs();
        if (akm.containsKey(Integer.toString(monsterIdFrom))) {
            if (!player.isGM() || player.isDebug()) {
                player.setHpMp(0);
            }
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
            MapleMapObject mapObject = map.getMapObject(objectId);
            if (!(mapObject instanceof MapleMonster)) {
                if (mapObject == null) {
                    return null;
                }
                throw new ClassCastException(String.format("'%s' took damage from map object %s in map %d", player.getName(), mapObject.getType().name(), player.getMapId()));
            }
            attacker = (MapleMonster) mapObject;
            List<LoseItem> loseItems;
            if (attacker.isBuffed(MonsterStatus.NEUTRALISE)) {
                if (player.getArcade() != null) {
                    player.getArcade().onHit(attacker.getId());
                }
                return null;
            }
            if (damage > 0) {
                loseItems = attacker.getStats().loseItem();
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
        }
        if (damageFrom != -1 && damageFrom != -2 && attacker != null) {
            MobAttackInfo attackInfo = MobAttackInfoFactory.getMobAttackInfo(attacker, damageFrom);
            if (attackInfo != null) {
                if (attackInfo.isDeadlyAttack()) {
                    mpattack = player.getMp() - 1;
                }
                mpattack += attackInfo.getMpBurn();
                MobSkill skill = MobSkillFactory.getMobSkill(attackInfo.getDiseaseSkill(), attackInfo.getDiseaseLevel());
                if (skill != null && damage > 0) {
                    skill.applyEffect(player, attacker, false);
                }

                attacker.setMp(attacker.getMp() - attackInfo.getMpCon());
                if (player.getBuffedValue(MapleBuffStat.MANA_REFLECTION) != null && damage > 0 && !attacker.isBoss()) {
                    int jobid = player.getJob().getId();
                    if (jobid == 212 || jobid == 222 || jobid == 232) {
                        int id = jobid * 10000 + 1002;
                        Skill mrSkill = SkillFactory.getSkill(id);
                        byte mrSkillLevel = player.getSkillLevel(mrSkill);
                        MapleStatEffect mrEffect = mrSkill.getEffect(mrSkillLevel);

                        if (mrSkillLevel > 0
                                && player.isBuffFrom(MapleBuffStat.MANA_REFLECTION, mrSkill)
                                && mrEffect.makeChanceResult()) {
                            int bouncedamage = (damage * mrEffect.getX() / 100);
                            if (bouncedamage > attacker.getMaxHp() / 5) {
                                bouncedamage = (int) (attacker.getMaxHp() / 5);
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

        Cheater.CheatEntry cheatEntry = player.getCheater().getCheatEntry(Cheats.GodMode);
        if (!player.isGM()) {
            if (damage == 0) {
                if (cheatEntry.getCheatCount() > 0 && cheatEntry.getCheatCount() % 15 == 0) {
                    cheatEntry.announce(player.getClient(), 25000, "{} has {} consecutive misses (possible god mode)", player.getName(), cheatEntry.getCheatCount());
                }
            } else {
                cheatEntry.resetCheatCount();
            }
        }
        cheatEntry.record();
        if (damage > 0 && !player.isHidden()) {
            Integer buffPowerGuard = player.getBuffedValue(MapleBuffStat.POWERGUARD);
            if (attacker != null && damageFrom == -1 && buffPowerGuard != null) { // PG works on bosses, but only at half of the rate.
                long bouncedamage = (int) (damage * (buffPowerGuard.doubleValue() / (attacker.isBoss() ? 200 : 100)));
                bouncedamage = Math.min(bouncedamage, attacker.getMaxHp() / 10);
                damage -= bouncedamage;
                map.damageMonster(player, attacker, bouncedamage);

                int localDamage = (int) Math.min(Integer.MAX_VALUE, bouncedamage);
                map.broadcastMessage(player, MaplePacketCreator.damageMonster(objectId, localDamage), false, true);
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
            player.getClient().announce(MaplePacketCreator.setSessionValue("energy", player.getDojoEnergy()));
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
