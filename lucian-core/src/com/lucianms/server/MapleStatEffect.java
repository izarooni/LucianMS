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
package com.lucianms.server;

import com.lucianms.client.*;
import com.lucianms.client.inventory.Item;
import com.lucianms.client.inventory.MapleInventory;
import com.lucianms.client.inventory.MapleInventoryType;
import com.lucianms.client.status.MonsterStatus;
import com.lucianms.client.status.MonsterStatusEffect;
import com.lucianms.constants.ItemConstants;
import com.lucianms.constants.skills.*;
import com.lucianms.scheduler.Task;
import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.server.life.MapleMonster;
import com.lucianms.server.maps.*;
import com.lucianms.server.world.MaplePartyCharacter;
import provider.MapleData;
import provider.MapleDataTool;
import provider.wz.XMLDomMapleData;
import tools.ArrayMap;
import tools.MaplePacketCreator;
import tools.Pair;

import java.awt.*;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Matze
 * @author Frz
 */
public class MapleStatEffect {

    private short watk, matk, wdef, mdef, acc, avoid, speed, jump;
    private short hp, mp;
    private double hpR, mpR;
    private short mpCon, hpCon;
    private int duration;
    private boolean overTime, repeatEffect;
    private int sourceid;
    private int moveTo;
    private boolean skill;
    private List<Pair<MapleBuffStat, Integer>> statups;
    private Map<MonsterStatus, Integer> monsterStatus;
    private int x, y, mobCount, moneyCon, cooldown, morphId = 0, ghost, fatigue, berserk, booster;
    private double prop;
    private int itemCon, itemConNo;
    private int damage, attackCount, fixdamage;
    private Point lt, rb;
    private byte bulletCount, bulletConsume;

    public static MapleStatEffect loadSkillEffectFromData(MapleData source, int skillid, boolean overtime) {
        return loadFromData(source, skillid, true, overtime);
    }

    public static MapleStatEffect loadItemEffectFromData(MapleData source, int itemid) {
        return loadFromData(source, itemid, false, false);
    }

    private static void addBuffStatPairToListIfNotZero(List<Pair<MapleBuffStat, Integer>> list, MapleBuffStat buffstat, Integer val) {
        if (val.intValue() != 0) {
            list.add(new Pair<>(buffstat, val));
        }
    }

    private static MapleStatEffect loadFromData(MapleData source, int sourceid, boolean skill, boolean overTime) {
        MapleStatEffect ret = new MapleStatEffect();
        ret.duration = MapleDataTool.getIntConvert("time", source, -1);
        ret.hp = (short) MapleDataTool.getInt("hp", source, 0);
        ret.hpR = MapleDataTool.getInt("hpR", source, 0) / 100.0;
        ret.mp = (short) MapleDataTool.getInt("mp", source, 0);
        ret.mpR = MapleDataTool.getInt("mpR", source, 0) / 100.0;
        ret.mpCon = (short) MapleDataTool.getInt("mpCon", source, 0);
        ret.hpCon = (short) MapleDataTool.getInt("hpCon", source, 0);
        int iprop = MapleDataTool.getInt("prop", source, 100);
        ret.prop = iprop / 100.0;
        ret.mobCount = MapleDataTool.getInt("mobCount", source, 1);
        ret.cooldown = MapleDataTool.getInt("cooltime", source, 0);
        ret.morphId = MapleDataTool.getInt("morph", source, 0);
        ret.ghost = MapleDataTool.getInt("ghost", source, 0);
        ret.fatigue = MapleDataTool.getInt("incFatigue", source, 0);
        ret.repeatEffect = MapleDataTool.getInt("repeatEffect", source, 0) > 0;

        ret.sourceid = sourceid;

        XMLDomMapleData parent = (XMLDomMapleData) source.getParent().getParent();
        MapleData summon = parent.getChildByPath("summon");

        ret.skill = skill;
        if (!ret.skill && ret.duration > -1) {
            ret.overTime = true;
        } else {
            ret.duration *= 1000; // items have their times stored in ms, of course

            // @formatter:off
            if ((sourceid == Marauder.TRANSFORMATION // pink bean
                    || sourceid == Buccaneer.SUPER_TRANSFORMATION) // super pink bean \o/
                    || (!ret.isMist()
                    && !ret.isMorph()
                    && !ret.isPoison()
                    && !ret.isMagicDoor()
                    && !ret.isDs()
                    && ret.isEnergy()
                    && sourceid != Sniper.PUPPET && sourceid != Ranger.PUPPET
                    && sourceid != Pirate.DASH && sourceid != ThunderBreaker.DASH
                    && sourceid != Shadower.NINJA_AMBUSH
                    && summon == null)) {
                ret.duration = Integer.MAX_VALUE;
            }
            ret.overTime = overTime;
            // @formatter:on
        }
        ArrayList<Pair<MapleBuffStat, Integer>> statups = new ArrayList<>();
        ret.watk = (short) MapleDataTool.getInt("pad", source, 0);
        ret.wdef = (short) MapleDataTool.getInt("pdd", source, 0);
        ret.matk = (short) MapleDataTool.getInt("mad", source, 0);
        ret.mdef = (short) MapleDataTool.getInt("mdd", source, 0);
        ret.acc = (short) MapleDataTool.getIntConvert("acc", source, 0);
        ret.avoid = (short) MapleDataTool.getInt("eva", source, 0);
        ret.speed = (short) MapleDataTool.getInt("speed", source, 0);
        ret.jump = (short) MapleDataTool.getInt("jump", source, 0);
        ret.berserk = MapleDataTool.getInt("berserk", source, 0);
        ret.booster = MapleDataTool.getInt("booster", source, 0);
        if (ret.overTime && ret.getSummonMovementType() == null) {
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.WATK, (int) ret.watk);
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.WDEF, (int) ret.wdef);
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.MATK, (int) ret.matk);
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.MDEF, (int) ret.mdef);
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.ACC, (int) ret.acc);
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.AVOID, (int) ret.avoid);
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.SPEED, (int) ret.speed);
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.JUMP, (int) ret.jump);
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.PYRAMID_PQ, ret.berserk);
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.BOOSTER, ret.booster);
        }
        MapleData ltd = source.getChildByPath("lt");
        if (ltd != null) {
            ret.lt = (Point) ltd.getData();
            ret.rb = (Point) source.getChildByPath("rb").getData();
        }
        int x = MapleDataTool.getInt("x", source, 0);
        ret.x = x;
        ret.y = MapleDataTool.getInt("y", source, 0);
        ret.damage = MapleDataTool.getIntConvert("damage", source, 100);
        ret.fixdamage = MapleDataTool.getIntConvert("fixdamage", source, -1);
        ret.attackCount = MapleDataTool.getIntConvert("attackCount", source, 1);
        ret.bulletCount = (byte) MapleDataTool.getIntConvert("bulletCount", source, 1);
        ret.bulletConsume = (byte) MapleDataTool.getIntConvert("bulletConsume", source, 0);
        ret.moneyCon = MapleDataTool.getIntConvert("moneyCon", source, 0);
        ret.itemCon = MapleDataTool.getInt("itemCon", source, 0);
        ret.itemConNo = MapleDataTool.getInt("itemConNo", source, 0);
        ret.moveTo = MapleDataTool.getInt("moveTo", source, -1);
        Map<MonsterStatus, Integer> monsterStatus = new ArrayMap<>();
        if (skill) {
            switch (sourceid) {
                // BEGINNER
                case Beginner.RECOVERY:
                case Noblesse.RECOVERY:
                case Legend.RECOVERY:
                case Evan.RECOVERY:
                    statups.add(new Pair<>(MapleBuffStat.RECOVERY, x));
                    break;
                case Beginner.ECHO_OF_HERO:
                case Noblesse.ECHO_OF_HERO:
                case Legend.ECHO_OF_HERO:
                case Evan.ECHO_OF_HERO:
                    statups.add(new Pair<>(MapleBuffStat.ECHO_OF_HERO, ret.x));
                    break;
                case Beginner.MONSTER_RIDER:
                case Noblesse.MONSTER_RIDER:
                case Legend.MONSTER_RIDER:
                case Corsair.BATTLESHIP:
                case Beginner.SPACESHIP:
                case Noblesse.SPACESHIP:
                case Beginner.YETI_RIDER:
                case Beginner.YETI_MOUNT:
                case Noblesse.YETI_RIDER:
                case Noblesse.YETI_MOUNT:
                case Legend.YETI_RIDER:
                case Legend.YETI_MOUNT:
                case Beginner.WITCH_BROOMSTICK:
                case Noblesse.WITCH_BROOMSTICK:
                case Legend.WITCH_BROOMSTICK:
                case Beginner.BARLOG_MOUNT:
                case Noblesse.BARLOG_MOUNT:
                case Legend.BARLOG_MOUNT:
                    statups.add(new Pair<>(MapleBuffStat.MONSTER_RIDING, sourceid));
                    break;
                case Beginner.POWER_EXPLOSION:
                case Noblesse.METEO_SHOWER:
                case Evan.BERSERK_FURY:
                    statups.add(new Pair<>(MapleBuffStat.BERSERK_FURY, 1));
                    break;
                case Beginner.INVINCIBILITY:
                case Noblesse.INVINCIBLE_BARRIER:
                case Legend.INVINCIBLE_BARRIER:
                case Evan.INVINCIBLE_BARRIER:
                    statups.add(new Pair<>(MapleBuffStat.DIVINE_BODY, 1));
                    break;
                case Fighter.POWER_GUARD:
                case Page.POWER_GUARD:
                    statups.add(new Pair<>(MapleBuffStat.POWERGUARD, x));
                    break;
                case Spearman.HYPER_BODY:
                case SuperGM.HYPER_BODY:
                    statups.add(new Pair<>(MapleBuffStat.HYPERBODYHP, x));
                    statups.add(new Pair<>(MapleBuffStat.HYPERBODYMP, ret.y));
                    break;
                case Crusader.COMBO_ATTACK:
                case DawnWarrior.COMBO_ATTACK:
                    statups.add(new Pair<>(MapleBuffStat.COMBO, 1));
                    break;
                case WhiteKnight.FLAME_CHARGE_BW:
                case WhiteKnight.BLIZZARD_CHARGE_BW:
                case WhiteKnight.LIGHTNING_CHARGE_BW:
                case WhiteKnight.FIRE_CHARGE_SWORD:
                case WhiteKnight.ICE_CHARGE_SWORD:
                case WhiteKnight.THUNDER_CHARGE_SWORD:
                case Paladin.DIVINE_CHARGE_BW:
                case Paladin.HOLY_CHARGE_SWORD:
                case DawnWarrior.SOUL_CHARGE:
                case ThunderBreaker.LIGHTNING_CHARGE:
                    statups.add(new Pair<>(MapleBuffStat.WK_CHARGE, x));
                    break;
                case DragonKnight.DRAGON_BLOOD:
                    statups.add(new Pair<>(MapleBuffStat.DRAGONBLOOD, ret.x));
                    break;
                case DragonKnight.DRAGON_ROAR:
                    ret.hpR = -x / 100.0;
                    break;
                case Hero.POWER_STANCE:
                case Paladin.POWER_STANCE:
                case DarkKnight.POWER_STANCE:
                case Aran.FREEZE_STANDING:
                    statups.add(new Pair<>(MapleBuffStat.STANCE, iprop));
                    break;
                case DawnWarrior.FINAL_ATTACK:
                case WindArcher.FINAL_ATTACK:
                    statups.add(new Pair<>(MapleBuffStat.FINALATTACK, x));
                    break;
                // MAGICIAN
                case Magician.MAGIC_GUARD:
                case BlazeWizard.MAGIC_GUARD:
                case Evan.MAGIC_GUARD:
                    statups.add(new Pair<>(MapleBuffStat.MAGIC_GUARD, x));
                    break;
                case Cleric.INVINCIBLE:
                    statups.add(new Pair<>(MapleBuffStat.INVINCIBLE, x));
                    break;
                case Priest.HOLY_SYMBOL:
                case SuperGM.HOLY_SYMBOL:
                    statups.add(new Pair<>(MapleBuffStat.HOLY_SYMBOL, x));
                    break;
                case FPArchMage.INFINITY:
                case ILArchMage.INFINITY:
                case Bishop.INFINITY:
                    statups.add(new Pair<>(MapleBuffStat.INFINITY, x));
                    break;
                case FPArchMage.MANA_REFLECTION:
                case ILArchMage.MANA_REFLECTION:
                case Bishop.MANA_REFLECTION:
                    statups.add(new Pair<>(MapleBuffStat.MANA_REFLECTION, 1));
                    break;
                case Bishop.HOLY_SHIELD:
                    statups.add(new Pair<>(MapleBuffStat.HOLY_SHIELD, x));
                    break;
                case BlazeWizard.ELEMENTAL_RESET:
                case Evan.ELEMENTAL_RESET:
                    statups.add(new Pair<>(MapleBuffStat.MAGIC_SHIELD, x));
                    break;
                case Evan.MAGIC_SHIELD:
                    statups.add(new Pair<>(MapleBuffStat.MAGIC_SHIELD, x));
                    break;
                case Evan.MAGIC_RESISTANCE:
                    statups.add(new Pair<>(MapleBuffStat.MAGIC_RESISTANCE, x));
                    break;
                case Evan.SLOW:
                    statups.add(new Pair<>(MapleBuffStat.SLOW, x));
                    // BOWMAN
                case Priest.MYSTIC_DOOR:
                case Hunter.SOUL_ARROW_BOW:
                case Crossbowman.SOUL_ARROW:
                case WindArcher.SOUL_ARROW:
                    statups.add(new Pair<>(MapleBuffStat.SOULARROW, x));
                    break;
                case Ranger.PUPPET:
                case Sniper.PUPPET:
                case WindArcher.PUPPET:
                case Outlaw.OCTOPUS:
                case Corsair.WRATH_OF_THE_OCTOPI:
                    statups.add(new Pair<>(MapleBuffStat.PUPPET, 1));
                    break;
                case Bowmaster.CONCENTRATE:
                    statups.add(new Pair<>(MapleBuffStat.CONCENTRATE, x));
                    break;
                case Bowmaster.HAMSTRING:
                    statups.add(new Pair<>(MapleBuffStat.HAMSTRING, x));
                    monsterStatus.put(MonsterStatus.SPEED, x);
                    break;
                case Marksman.BLIND:
                    statups.add(new Pair<>(MapleBuffStat.BLIND, x));
                    monsterStatus.put(MonsterStatus.ACC, x);
                    break;
                case Bowmaster.SHARP_EYES:
                case Marksman.SHARP_EYES:
                    statups.add(new Pair<>(MapleBuffStat.SHARP_EYES, ret.x << 8 | ret.y));
                    break;
                // THIEF
                case Rogue.DARK_SIGHT:
                case WindArcher.WIND_WALK:
                case NightWalker.DARK_SIGHT:
                    statups.add(new Pair<>(MapleBuffStat.DARKSIGHT, x));
                    break;
                case Hermit.MESO_UP:
                    statups.add(new Pair<>(MapleBuffStat.MESOUP, x));
                    break;
                case Hermit.SHADOW_PARTNER:
                case NightWalker.SHADOW_PARTNER:
                    statups.add(new Pair<>(MapleBuffStat.SHADOWPARTNER, x));
                    break;
                case ChiefBandit.MESO_GUARD:
                    statups.add(new Pair<>(MapleBuffStat.MESOGUARD, x));
                    break;
                case ChiefBandit.PICKPOCKET:
                    statups.add(new Pair<>(MapleBuffStat.PICKPOCKET, x));
                    break;
                case NightLord.SHADOW_STARS:
                    statups.add(new Pair<>(MapleBuffStat.SHADOW_CLAW, 0));
                    break;
                // PIRATE
                case Pirate.DASH:
                case ThunderBreaker.DASH:
                case Beginner.SPACE_DASH:
                case Noblesse.SPACE_DASH:
                    statups.add(new Pair<>(MapleBuffStat.DASH2, ret.x));
                    statups.add(new Pair<>(MapleBuffStat.DASH, ret.y));
                    break;
                case Corsair.SPEED_INFUSION:
                case Buccaneer.SPEED_INFUSION:
                case ThunderBreaker.SPEED_INFUSION:
                    statups.add(new Pair<>(MapleBuffStat.SPEED_INFUSION, x));
                    break;
                case Outlaw.HOMING_BEACON:
                case Corsair.BULLSEYE:
                    statups.add(new Pair<>(MapleBuffStat.HOMING_BEACON, x));
                    break;
                case ThunderBreaker.SPARK:
                    statups.add(new Pair<>(MapleBuffStat.SPARK, x));
                    break;
                // MULTIPLE
                case Aran.POLEARM_BOOSTER:
                case Fighter.AXE_BOOSTER:
                case Fighter.SWORD_BOOSTER:
                case Page.BW_BOOSTER:
                case Page.SWORD_BOOSTER:
                case Spearman.POLE_ARM_BOOSTER:
                case Spearman.SPEAR_BOOSTER:
                case Hunter.BOW_BOOSTER:
                case Crossbowman.CROSSBOW_BOOSTER:
                case Assassin.CLAW_BOOSTER:
                case Bandit.DAGGER_BOOSTER:
                case FPMage.SPELL_BOOSTER:
                case ILMage.SPELL_BOOSTER:
                case Brawler.KNUCKLER_BOOSTER:
                case Gunslinger.GUN_BOOSTER:
                case DawnWarrior.SWORD_BOOSTER:
                case BlazeWizard.SPELL_BOOSTER:
                case WindArcher.BOW_BOOSTER:
                case NightWalker.CLAW_BOOSTER:
                case ThunderBreaker.KNUCKLE_BOOSTER:
                case Evan.MAGIC_BOOSTER:
                    statups.add(new Pair<>(MapleBuffStat.BOOSTER, x));
                    break;
                case Hero.MAPLE_WARRIOR:
                case Paladin.MAPLE_WARRIOR:
                case DarkKnight.MAPLE_WARRIOR:
                case FPArchMage.MAPLE_WARRIOR:
                case ILArchMage.MAPLE_WARRIOR:
                case Bishop.MAPLE_WARRIOR:
                case Bowmaster.MAPLE_WARRIOR:
                case Marksman.MAPLE_WARRIOR:
                case NightLord.MAPLE_WARRIOR:
                case Shadower.MAPLE_WARRIOR:
                case Corsair.MAPLE_WARRIOR:
                case Buccaneer.MAPLE_WARRIOR:
                case Aran.MAPLE_WARRIOR:
                case Evan.MAPLE_WARRIOR:
                    statups.add(new Pair<>(MapleBuffStat.MAPLE_WARRIOR, ret.x));
                    break;
                // SUMMON
                case Ranger.SILVER_HAWK:
                case Sniper.GOLDEN_EAGLE:
                    statups.add(new Pair<>(MapleBuffStat.SUMMON, 1));
                    monsterStatus.put(MonsterStatus.STUN, 1);
                    break;
                case FPArchMage.ELQUINES:
                case Marksman.FROSTPREY:
                    statups.add(new Pair<>(MapleBuffStat.SUMMON, 1));
                    monsterStatus.put(MonsterStatus.FREEZE, 1);
                    break;
                case Priest.SUMMON_DRAGON:
                case Bowmaster.PHOENIX:
                case ILArchMage.IFRIT:
                case Bishop.BAHAMUT:
                case DarkKnight.BEHOLDER:
                case Outlaw.GAVIOTA:
                case DawnWarrior.SOUL:
                case BlazeWizard.FLAME:
                case WindArcher.STORM:
                case NightWalker.DARKNESS:
                case ThunderBreaker.LIGHTNING:
                case BlazeWizard.IFRIT:
                    statups.add(new Pair<>(MapleBuffStat.SUMMON, 1));
                    break;
                // ----------------------------- MONSTER STATUS ---------------------------------- //
                case Crusader.ARMOR_CRASH:
                case DragonKnight.POWER_CRASH:
                case WhiteKnight.MAGIC_CRASH:
                    monsterStatus.put(MonsterStatus.SEAL_SKILL, 1);
                    break;
                case Rogue.DISORDER:
                    monsterStatus.put(MonsterStatus.WATK, ret.x);
                    monsterStatus.put(MonsterStatus.WDEF, ret.y);
                    break;
                case Corsair.HYPNOTIZE:
                    monsterStatus.put(MonsterStatus.INERTMOB, 1);
                    break;
                case NightLord.NINJA_AMBUSH:
                case Shadower.NINJA_AMBUSH:
                    monsterStatus.put(MonsterStatus.NINJA_AMBUSH, ret.damage);
                    break;
                case Page.THREATEN:
                    monsterStatus.put(MonsterStatus.WATK, ret.x);
                    monsterStatus.put(MonsterStatus.WDEF, ret.y);
                    break;
                case Crusader.COMA_AXE:
                case Crusader.COMA_SWORD:
                case Crusader.SHOUT:
                case WhiteKnight.CHARGED_BLOW:
                case Hunter.ARROW_BOMB_BOW:
                case ChiefBandit.ASSAULTER:
                case Shadower.BOOMERANG_STEP:
                case Brawler.BACKSPIN_BLOW:
                case Brawler.DOUBLE_UPPERCUT:
                case Buccaneer.DEMOLITION:
                case Buccaneer.SNATCH:
                case Buccaneer.BARRAGE:
                case Gunslinger.BLANK_SHOT:
                case DawnWarrior.COMA:
                case Aran.ROLLING_SPIN:
                case Evan.FIRE_BREATH:
                case Evan.BLAZE:
                    monsterStatus.put(MonsterStatus.STUN, 1);
                    break;
                case NightLord.TAUNT:
                case Shadower.TAUNT:
                    monsterStatus.put(MonsterStatus.SHOWDOWN, ret.x);
                    monsterStatus.put(MonsterStatus.MDEF, ret.x);
                    monsterStatus.put(MonsterStatus.WDEF, ret.x);
                    break;
                case ILWizard.COLD_BEAM:
                case ILMage.ICE_STRIKE:
                case ILArchMage.BLIZZARD:
                case ILMage.ELEMENT_COMPOSITION:
                case Sniper.BLIZZARD:
                case Outlaw.ICE_SPLITTER:
                case FPArchMage.PARALYZE:
                case Aran.COMBO_TEMPEST:
                case Evan.ICE_BREATH:
                    monsterStatus.put(MonsterStatus.FREEZE, 1);
                    ret.duration *= 2; // freezing skills are a little strange
                    break;
                case FPWizard.SLOW:
                case ILWizard.SLOW:
                case BlazeWizard.SLOW:
                    monsterStatus.put(MonsterStatus.SPEED, ret.x);
                    break;
                case FPWizard.POISON_BREATH:
                case FPMage.ELEMENT_COMPOSITION:
                    monsterStatus.put(MonsterStatus.POISON, 1);
                    break;
                case Priest.DOOM:
                    monsterStatus.put(MonsterStatus.DOOM, 1);
                    break;
                case ILMage.SEAL:
                case FPMage.SEAL:
                    monsterStatus.put(MonsterStatus.SEAL, 1);
                    break;
                case Hermit.SHADOW_WEB: // shadow web
                case NightWalker.SHADOW_WEB:
                    monsterStatus.put(MonsterStatus.SHADOW_WEB, 1);
                    break;
                case FPArchMage.FIRE_DEMON:
                case ILArchMage.ICE_DEMON:
                    monsterStatus.put(MonsterStatus.POISON, 1);
                    monsterStatus.put(MonsterStatus.FREEZE, 1);
                    break;
                case Evan.PHANTOM_IMPRINT:
                    monsterStatus.put(MonsterStatus.PHANTOM_IMPRINT, x);
                    //ARAN
                case Aran.COMBO_ABILITY:
                    statups.add(new Pair<>(MapleBuffStat.ARAN_COMBO, 100));
                    break;
                case Aran.COMBO_BARRIER:
                    statups.add(new Pair<>(MapleBuffStat.COMBO_BARRIER, ret.x));
                    break;
                case Aran.COMBO_DRAIN:
                    statups.add(new Pair<>(MapleBuffStat.COMBO_DRAIN, ret.x));
                    break;
                case Aran.SMART_KNOCKBACK:
                    statups.add(new Pair<>(MapleBuffStat.SMART_KNOCKBACK, ret.x));
                    break;
                case Aran.BODY_PRESSURE:
                    statups.add(new Pair<>(MapleBuffStat.BODY_PRESSURE, ret.x));
                    break;
                case Aran.SNOW_CHARGE:
                    statups.add(new Pair<>(MapleBuffStat.WK_CHARGE, ret.duration));
                    break;
                default:
                    break;
            }
        }
        if (ret.isMorph()) {
            statups.add(new Pair<>(MapleBuffStat.MORPH, ret.getMorph()));
        }
        if (ret.ghost > 0 && !skill) {
            statups.add(new Pair<>(MapleBuffStat.GHOST_MORPH, ret.ghost));
        }
        ret.monsterStatus = monsterStatus;
        statups.trimToSize();
        ret.statups = statups;
        return ret;
    }

    private boolean isEnergy() {
        return sourceid == ThunderBreaker.ENERGY_CHARGE || sourceid == Buccaneer.ENERGY_ORB;
    }

    /**
     * @param applyto
     * @param obj
     * @param attack  damage done by the skill
     */
    public void applyPassive(MapleCharacter applyto, MapleMapObject obj, int attack) {
        if (makeChanceResult()) {
            switch (sourceid) { // MP eater
                case FPWizard.MP_EATER:
                case ILWizard.MP_EATER:
                case Cleric.MP_EATER:
                    if (obj == null || obj.getType() != MapleMapObjectType.MONSTER) {
                        return;
                    }
                    MapleMonster mob = (MapleMonster) obj; // x is absorb percentage
                    if (!mob.isBoss()) {
                        int absorbMp = Math.min((int) (mob.getMaxMp() * (getX() / 100.0)), mob.getMp());
                        if (absorbMp > 0) {
                            mob.setMp(mob.getMp() - absorbMp);
                            applyto.addMP(absorbMp);
                            applyto.getClient().announce(MaplePacketCreator.showOwnBuffEffect(sourceid, 1));
                            applyto.getMap().broadcastMessage(applyto, MaplePacketCreator.showBuffeffect(applyto.getId(), sourceid, 1), false);
                        }
                    }
                    break;
            }
        }
    }

    public boolean applyTo(MapleCharacter chr) {
        return applyTo(chr, chr, true, null);
    }

    public boolean applyTo(MapleCharacter chr, Point pos) {
        return applyTo(chr, chr, true, pos);
    }

    private boolean applyTo(MapleCharacter applyfrom, MapleCharacter applyto, boolean primary, Point pos) {
        if (skill && sourceid == SuperGM.HIDE) {
            applyto.toggleHide();
            return true;
        }
        int hpchange = calcHPChange(applyfrom, primary);
        int mpchange = calcMPChange(applyfrom, primary);

        if (primary) {
            if (itemConNo != 0) {
                MapleInventoryManipulator.removeById(applyto.getClient(), MapleItemInformationProvider.getInstance().getInventoryType(itemCon), itemCon, itemConNo, false, true);
            }
        }
        List<Pair<MapleStat, Integer>> hpmpupdate = new ArrayList<>(2);
        if (!primary && isResurrection()) {
            hpchange = applyto.getMaxHp();
            applyto.setStance(0);
            applyto.getMap().broadcastMessage(applyto, MaplePacketCreator.removePlayerFromMap(applyto.getId()), false);
            applyto.getMap().broadcastMessage(applyto, MaplePacketCreator.spawnPlayerMapobject(applyto), false);
        }
        if (isDispel() && makeChanceResult()) {
            applyto.dispelDebuffs();
        } else if (isHeroWill()) {
            applyto.dispelDebuff(MapleDisease.SEDUCE);
        }
        if (isComboReset()) {
            applyto.setCombo((short) 0);
        }
        /*if (applyfrom.getMp() < getMpCon()) {
         AutobanFactory.MPCON.addPoint(applyfrom.getAutobanManager(), "mpCon hack for skill:" + sourceid + "; Player MP: " + applyto.getMp() + " MP Needed: " + getMpCon());
         } */
        if (hpchange != 0) {
            if (hpchange < 0 && (-hpchange) > applyto.getHp()) {
                return false;
            }
            int newHp = applyto.getHp() + hpchange;
            if (newHp < 1) {
                newHp = 1;
            }
            applyto.setHp(newHp);
            hpmpupdate.add(new Pair<>(MapleStat.HP, applyto.getHp()));
        }
        int newMp = applyto.getMp() + mpchange;
        if (mpchange != 0) {
            if (mpchange < 0 && -mpchange > applyto.getMp()) {
                return false;
            }

            applyto.setMp(newMp);
            hpmpupdate.add(new Pair<>(MapleStat.MP, applyto.getMp()));
        }
        applyto.getClient().announce(MaplePacketCreator.updatePlayerStats(hpmpupdate, true, applyto));
        if (moveTo != -1) {
            if (moveTo != applyto.getMapId()) {
                MapleMap target;
                if (moveTo == 999999999) {
                    target = applyto.getMap().getReturnMap();
                } else {
                    target = applyto.getClient().getWorldServer().getChannel(applyto.getClient().getChannel()).getMap(moveTo);
                    int targetid = target.getId() / 10000000;
                    if (targetid != 60 && applyto.getMapId() / 10000000 != 61 && targetid != applyto.getMapId() / 10000000 && targetid != 21 && targetid != 20 && targetid != 12 && (applyto.getMapId() / 10000000 != 10 && applyto.getMapId() / 10000000 != 12)) {
                        return false;
                    }
                }
                applyto.changeMap(target);
            } else {
                return false;
            }

        }
        if (isShadowClaw()) {
            int projectile = 0;
            MapleInventory use = applyto.getInventory(MapleInventoryType.USE);
            for (int i = 1; i <= use.getSlotLimit(); i++) { // impose order...
                Item item = use.getItem((short) i);
                if (item != null) {
                    if (ItemConstants.isThrowingStar(item.getItemId()) && item.getQuantity() >= 200) {
                        projectile = item.getItemId();
                        break;
                    }
                }
            }
            if (projectile == 0) {
                return false;
            } else {
                MapleInventoryManipulator.removeById(applyto.getClient(), MapleInventoryType.USE, projectile, 200, false, true);
            }

        }
        SummonMovementType summonMovementType = getSummonMovementType();
        if (overTime || isCygnusFA() || summonMovementType != null) {
            applyBuffEffect(applyfrom, applyto, primary);
        }

        if (primary && (overTime || isHeal())) {
            applyBuff(applyfrom);
        }

        if (primary && isMonsterBuff()) {
            applyMonsterBuff(applyfrom);
        }

        if (this.getFatigue() != 0) {
            applyto.getMount().setTiredness(applyto.getMount().getTiredness() + this.getFatigue());
        }

        if (summonMovementType != null && pos != null) {
            final MapleSummon tosummon = new MapleSummon(applyfrom, sourceid, pos, summonMovementType);
            applyfrom.getMap().spawnSummon(tosummon);
            applyfrom.addSummon(sourceid, tosummon);
            tosummon.addHP(x);
            if (isBeholder()) {
                tosummon.addHP(1);
            }
        }
        if (isMagicDoor() && !FieldLimit.DOOR.check(applyto.getMap().getFieldLimit())) { // Magic Door
            int y = applyto.getFh();
            if (y == 0) {
                y = applyto.getPosition().y;
            }
            Point doorPosition = new Point(applyto.getPosition().x, y);
            MapleDoor door = new MapleDoor(applyto, doorPosition);
            if (applyto.getParty() != null) {// out of town door
                for (MaplePartyCharacter partyMembers : applyto.getParty().getMembers()) {
                    partyMembers.getPlayer().addDoor(door);
                    partyMembers.updateDoor(door);
                }
                applyto.silentPartyUpdate();
            } else {
                applyto.addDoor(door);
            }
            applyto.getMap().spawnDoor(door);
            door = new MapleDoor(door); //The town door
            if (applyto.getParty() != null) {// update town doors
                for (MaplePartyCharacter partyMembers : applyto.getParty().getMembers()) {
                    partyMembers.getPlayer().addDoor(door);
                    partyMembers.updateDoor(door);
                }
                applyto.silentPartyUpdate();
            } else {
                applyto.addDoor(door);
            }
            door.getTown().spawnDoor(door);
            applyto.disableDoor();
        } else if (isMist()) {
            Rectangle bounds = calculateBoundingBox(sourceid == NightWalker.POISON_BOMB ? pos : applyfrom.getPosition(), applyfrom.isFacingLeft());
            MapleMist mist = new MapleMist(bounds, applyfrom, this);
            applyfrom.getMap().spawnMist(mist, getDuration(), mist.isPoisonMist(), false, mist.isRecoveryMist());
        } else if (isTimeLeap()) {
            applyto.removeAllCooldownsExcept(Buccaneer.TIME_LEAP, true);
        }
        return true;
    }

    private void applyBuff(MapleCharacter applyfrom) {
        if (isPartyBuff() && (applyfrom.getParty() != null || isGmBuff())) {
            Rectangle bounds = calculateBoundingBox(applyfrom.getPosition(), applyfrom.isFacingLeft());
            List<MapleMapObject> affecteds = applyfrom.getMap().getMapObjectsInRect(bounds, Collections.singletonList(MapleMapObjectType.PLAYER));
            List<MapleCharacter> affectedp = new ArrayList<>(affecteds.size());
            for (MapleMapObject affectedmo : affecteds) {
                MapleCharacter affected = (MapleCharacter) affectedmo;
                if (affected != applyfrom && (isGmBuff() || applyfrom.getParty().equals(affected.getParty()))) {
                    if ((isResurrection() && !affected.isAlive()) || (!isResurrection() && affected.isAlive())) {
                        affectedp.add(affected);
                    }
                }
            }
            for (MapleCharacter affected : affectedp) {
                applyTo(applyfrom, affected, false, null);
                affected.getClient().announce(MaplePacketCreator.showOwnBuffEffect(sourceid, 2));
                affected.getMap().broadcastMessage(affected, MaplePacketCreator.showBuffeffect(affected.getId(), sourceid, 2), false);
            }
        }
    }

    private void applyMonsterBuff(MapleCharacter applyfrom) {
        Rectangle bounds = calculateBoundingBox(applyfrom.getPosition(), applyfrom.isFacingLeft());
        List<MapleMapObject> affected = applyfrom.getMap().getMapObjectsInRect(bounds, Collections.singletonList(MapleMapObjectType.MONSTER));
        Skill skill_ = SkillFactory.getSkill(sourceid);
        int i = 0;
        for (MapleMapObject mo : affected) {
            MapleMonster monster = (MapleMonster) mo;
            if (isDispel()) {
                monster.debuffMob(skill_.getId());
            } else {
                if (makeChanceResult()) {
                    monster.applyStatus(applyfrom, new MonsterStatusEffect(getMonsterStati(), skill_, null, false), isPoison(), getDuration());
                    if (isCrash()) {
                        monster.debuffMob(skill_.getId());
                    }
                }
            }
            i++;
            if (i >= mobCount) {
                break;
            }
        }
    }

    private Rectangle calculateBoundingBox(Point posFrom, boolean facingLeft) {
        Point mylt;
        Point myrb;
        if (facingLeft) {
            mylt = new Point(lt.x + posFrom.x, lt.y + posFrom.y);
            myrb = new Point(rb.x + posFrom.x, rb.y + posFrom.y);
        } else {
            myrb = new Point(-lt.x + posFrom.x, rb.y + posFrom.y);
            mylt = new Point(-rb.x + posFrom.x, lt.y + posFrom.y);
        }
        Rectangle bounds = new Rectangle(mylt.x, mylt.y, myrb.x - mylt.x, myrb.y - mylt.y);
        return bounds;
    }

    public void silentApplyBuff(MapleCharacter chr, long starttime) {
        int localDuration = duration;
        localDuration = alchemistModifyVal(chr, localDuration, false);
        CancelEffectAction cancelAction = new CancelEffectAction(chr, this, starttime);
        Task task = TaskExecutor.createTask(cancelAction, ((starttime + localDuration) - System.currentTimeMillis()));
        chr.registerEffect(this, starttime, task);
        SummonMovementType summonMovementType = getSummonMovementType();
        if (summonMovementType != null) {
            final MapleSummon tosummon = new MapleSummon(chr, sourceid, chr.getPosition(), summonMovementType);
            if (!tosummon.isStationary()) {
                chr.addSummon(sourceid, tosummon);
                tosummon.addHP(x);
            }
        }
        if (sourceid == Corsair.BATTLESHIP) {
            chr.announce(MaplePacketCreator.skillCooldown(5221999, chr.getBattleshipHp()));
        }
    }

    public final void applyComboBuff(final MapleCharacter applyto, int combo) {
        final List<Pair<MapleBuffStat, Integer>> stat = Collections.singletonList(new Pair<>(MapleBuffStat.ARAN_COMBO, combo));
        applyto.getClient().announce(MaplePacketCreator.giveBuff(sourceid, 99999, stat));

        final long starttime = System.currentTimeMillis();
        //	final CancelEffectAction cancelAction = new CancelEffectAction(applyto, this, starttime);
        //	final ScheduledFuture<?> schedule = TimerManager.getInstance().schedule(cancelAction, ((starttime + 99999) - System.currentTimeMillis()));
        applyto.registerEffect(this, starttime, null);
    }

    private void applyBuffEffect(MapleCharacter applyfrom, MapleCharacter applyto, boolean primary) {
        if (!isMonsterRiding()) {
            applyto.cancelEffect(this, true, -1);
        }

        List<Pair<MapleBuffStat, Integer>> localstatups = statups;
        int localDuration = duration;
        int localsourceid = sourceid;
        int seconds = localDuration / 1000;
        MapleMount givemount = null;
        if (isMonsterRiding()) {
            int ridingLevel = 0;
            Item mount = applyfrom.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -18);
            if (mount != null) {
                ridingLevel = mount.getItemId();
            }
            if (sourceid == Corsair.BATTLESHIP) {
                ridingLevel = 1932000;
            } else if (sourceid == Beginner.SPACESHIP || sourceid == Noblesse.SPACESHIP) {
                ridingLevel = 1932000 + applyto.getSkillLevel(sourceid);
            } else if (sourceid == Beginner.YETI_RIDER || sourceid == Noblesse.YETI_RIDER || sourceid == Legend.YETI_RIDER) {
                ridingLevel = 1932003;
            } else if (sourceid == Beginner.YETI_MOUNT || sourceid == Noblesse.YETI_MOUNT || sourceid == Legend.YETI_MOUNT) {
                ridingLevel = 1932004;
            } else if (sourceid == Beginner.WITCH_BROOMSTICK || sourceid == Noblesse.WITCH_BROOMSTICK || sourceid == Legend.WITCH_BROOMSTICK) {
                ridingLevel = 1932005;
            } else if (sourceid == Beginner.BARLOG_MOUNT || sourceid == Noblesse.BARLOG_MOUNT || sourceid == Legend.BARLOG_MOUNT) {
                ridingLevel = 1932010;
            } else {
                if (applyto.getMount() == null) {
                    applyto.mount(ridingLevel, sourceid);
                }
                applyto.getMount().startSchedule();
            }
            if (sourceid == Corsair.BATTLESHIP) {
                givemount = new MapleMount(applyto, 1932000, sourceid);
            } else if (sourceid == Beginner.SPACESHIP || sourceid == Noblesse.SPACESHIP) {
                givemount = new MapleMount(applyto, 1932000 + applyto.getSkillLevel(sourceid), sourceid);
            } else if (sourceid == Beginner.YETI_RIDER || sourceid == Noblesse.YETI_RIDER || sourceid == Legend.YETI_RIDER) {
                givemount = new MapleMount(applyto, 1932003, sourceid);
            } else if (sourceid == Beginner.YETI_MOUNT || sourceid == Noblesse.YETI_MOUNT || sourceid == Legend.YETI_MOUNT) {
                givemount = new MapleMount(applyto, 1932004, sourceid);
            } else if (sourceid == Beginner.WITCH_BROOMSTICK || sourceid == Noblesse.WITCH_BROOMSTICK || sourceid == Legend.WITCH_BROOMSTICK) {
                givemount = new MapleMount(applyto, 1932005, sourceid);
            } else if (sourceid == Beginner.BARLOG_MOUNT || sourceid == Noblesse.BARLOG_MOUNT || sourceid == Legend.BARLOG_MOUNT) {
                givemount = new MapleMount(applyto, 1932010, sourceid);
            } else {
                givemount = applyto.getMount();
            }
            localDuration = sourceid;
            localsourceid = ridingLevel;
            localstatups = Collections.singletonList(new Pair<>(MapleBuffStat.MONSTER_RIDING, 0));
        } else if (isSkillMorph()) {
            localstatups = Collections.singletonList(new Pair<>(MapleBuffStat.MORPH, getMorph(applyto)));
        }
        if (primary) {
            localDuration = alchemistModifyVal(applyfrom, localDuration, false);
            applyto.getMap().broadcastMessage(applyto, MaplePacketCreator.showBuffeffect(applyto.getId(), sourceid, 1, (byte) 3), false);
        }
        if (localstatups.size() > 0) {
            byte[] buff = null;
            byte[] mbuff = null;
            if (getSummonMovementType() == null) {
                buff = MaplePacketCreator.giveBuff((skill ? sourceid : -sourceid), localDuration, localstatups);
            }
            if (isDash()) {
                buff = MaplePacketCreator.givePirateBuff(statups, sourceid, seconds);
                mbuff = MaplePacketCreator.giveForgeinPirateBuff(applyto.getId(), sourceid, seconds, localstatups);
            } else if (isInfusion()) {
                buff = MaplePacketCreator.givePirateBuff(localstatups, sourceid, seconds);
                mbuff = MaplePacketCreator.giveForgeinPirateBuff(applyto.getId(), sourceid, seconds, localstatups);
            } else if (isDs()) {
                List<Pair<MapleBuffStat, Integer>> dsstat = Collections.singletonList(new Pair<>(MapleBuffStat.DARKSIGHT, 0));
                mbuff = MaplePacketCreator.giveForeignBuff(applyto.getId(), dsstat);
            } else if (isCombo()) {
                mbuff = MaplePacketCreator.giveForeignBuff(applyto.getId(), statups);
            } else if (isMonsterRiding()) {
                buff = MaplePacketCreator.giveBuff(localsourceid, localDuration, localstatups);
                mbuff = MaplePacketCreator.showMonsterRiding(applyto.getId(), givemount);
                localDuration = duration;
                if (sourceid == Corsair.BATTLESHIP) {//hp
                    if (applyto.getBattleshipHp() == 0) {
                        applyto.resetBattleshipHp();
                    }
                }
            } else if (isShadowPartner()) {
                List<Pair<MapleBuffStat, Integer>> stat = Collections.singletonList(new Pair<>(MapleBuffStat.SHADOWPARTNER, 0));
                mbuff = MaplePacketCreator.giveForeignBuff(applyto.getId(), stat);
            } else if (isSoulArrow()) {
                List<Pair<MapleBuffStat, Integer>> stat = Collections.singletonList(new Pair<>(MapleBuffStat.SOULARROW, 0));
                mbuff = MaplePacketCreator.giveForeignBuff(applyto.getId(), stat);
            } else if (isEnrage()) {
                applyto.handleOrbconsume();
            } else if (isMorph()) {
                List<Pair<MapleBuffStat, Integer>> stat = Collections.singletonList(new Pair<>(MapleBuffStat.MORPH, getMorph(applyto)));
                mbuff = MaplePacketCreator.giveForeignBuff(applyto.getId(), stat);
            }
            long starttime = System.currentTimeMillis();
            CancelEffectAction cancelAction = new CancelEffectAction(applyto, this, starttime);
            Task task = TaskExecutor.createTask(cancelAction, localDuration);
            applyto.registerEffect(this, starttime, task);

            if (buff != null) {
                if (!hasNoIcon()) { //Thanks flav for such a simple release! :)
                    applyto.getClient().announce(buff);
                }
            }
            if (mbuff != null) {
                applyto.getMap().broadcastMessage(applyto, mbuff, false);
            }
            if (sourceid == Corsair.BATTLESHIP) {
                applyto.announce(MaplePacketCreator.skillCooldown(5221999, applyto.getBattleshipHp() / 10));
            }
        }
    }

    private int calcHPChange(MapleCharacter applyfrom, boolean primary) {
        int hpchange = 0;
        if (hp != 0) {
            if (!skill) {
                if (primary) {
                    hpchange += alchemistModifyVal(applyfrom, hp, true);
                } else {
                    hpchange += hp;
                }
            } else {
                hpchange += makeHealHP(hp / 100.0, applyfrom.getTotalMagic(), 3, 5);
            }
        }
        if (hpR != 0) {
            hpchange += (int) (applyfrom.getCurrentMaxHp() * hpR);
            applyfrom.checkBerserk();
        }
        if (primary) {
            if (hpCon != 0) {
                hpchange -= hpCon;
            }
        }
        if (isChakra()) {
            hpchange += makeHealHP(getY() / 100.0, applyfrom.getTotalLuk(), 2.3, 3.5);
        } else if (sourceid == SuperGM.HEAL_PLUS_DISPEL) {
            hpchange += (applyfrom.getMaxHp() - applyfrom.getHp());
        }

        return hpchange;
    }

    private int makeHealHP(double rate, double stat, double lowerfactor, double upperfactor) {
        return (int) ((Math.random() * ((int) (stat * upperfactor * rate) - (int) (stat * lowerfactor * rate) + 1)) + (int) (stat * lowerfactor * rate));
    }

    private int calcMPChange(MapleCharacter applyfrom, boolean primary) {
        int mpchange = 0;
        if (mp != 0) {
            if (primary) {
                mpchange += alchemistModifyVal(applyfrom, mp, true);
            } else {
                mpchange += mp;
            }
        }
        if (mpR != 0) {
            mpchange += (int) (applyfrom.getCurrentMaxMp() * mpR);
        }
        if (primary) {
            if (mpCon != 0) {
                double mod = 1.0;
                boolean isAFpMage = applyfrom.getJob().isA(MapleJob.FP_MAGE);
                boolean isCygnus = applyfrom.getJob().isA(MapleJob.BLAZEWIZARD2);
                boolean isEvan = applyfrom.getJob().isA(MapleJob.EVAN7);
                if (isAFpMage || isCygnus || isEvan || applyfrom.getJob().isA(MapleJob.IL_MAGE)) {
                    Skill amp = isAFpMage ? SkillFactory.getSkill(FPMage.ELEMENT_AMPLIFICATION) : (isCygnus ? SkillFactory.getSkill(BlazeWizard.ELEMENT_AMPLIFICATION) : (isEvan ? SkillFactory.getSkill(Evan.MAGIC_AMPLIFICATION) : SkillFactory.getSkill(ILMage.ELEMENT_AMPLIFICATION)));
                    int ampLevel = applyfrom.getSkillLevel(amp);
                    if (ampLevel > 0) {
                        mod = amp.getEffect(ampLevel).getX() / 100.0;
                    }
                }
                mpchange -= mpCon * mod;
                if (applyfrom.getBuffedValue(MapleBuffStat.INFINITY) != null) {
                    mpchange = 0;
                } else if (applyfrom.getBuffedValue(MapleBuffStat.CONCENTRATE) != null) {
                    mpchange -= (int) (mpchange * (applyfrom.getBuffedValue(MapleBuffStat.CONCENTRATE).doubleValue() / 100));
                }
            }
        }
        if (sourceid == SuperGM.HEAL_PLUS_DISPEL) {
            mpchange += (applyfrom.getMaxMp() - applyfrom.getMp());
        }

        return mpchange;
    }

    private int alchemistModifyVal(MapleCharacter chr, int val, boolean withX) {
        if (!skill && (chr.getJob().isA(MapleJob.HERMIT) || chr.getJob().isA(MapleJob.NIGHTWALKER3))) {
            MapleStatEffect alchemistEffect = getAlchemistEffect(chr);
            if (alchemistEffect != null) {
                return (int) (val * ((withX ? alchemistEffect.getX() : alchemistEffect.getY()) / 100.0));
            }
        }
        return val;
    }

    private MapleStatEffect getAlchemistEffect(MapleCharacter chr) {
        int id = Hermit.ALCHEMIST;
        if (chr.isCygnus()) {
            id = NightWalker.ALCHEMIST;
        }
        int alchemistLevel = chr.getSkillLevel(SkillFactory.getSkill(id));
        return alchemistLevel == 0 ? null : SkillFactory.getSkill(id).getEffect(alchemistLevel);
    }

    private boolean isGmBuff() {
        switch (sourceid) {
            case Beginner.ECHO_OF_HERO:
            case Noblesse.ECHO_OF_HERO:
            case Legend.ECHO_OF_HERO:
            case Evan.ECHO_OF_HERO:
            case SuperGM.HEAL_PLUS_DISPEL:
            case SuperGM.HASTE_SUPER:
            case SuperGM.HOLY_SYMBOL:
            case SuperGM.BLESS:
            case SuperGM.RESURRECTION:
            case SuperGM.HYPER_BODY:
                return true;
            default:
                return false;
        }
    }

    private boolean isMonsterBuff() {
        if (!skill) {
            return false;
        }
        switch (sourceid) {
            case Page.THREATEN:
            case FPWizard.SLOW:
            case ILWizard.SLOW:
            case FPMage.SEAL:
            case ILMage.SEAL:
            case Priest.DOOM:
            case Hermit.SHADOW_WEB:
            case NightLord.NINJA_AMBUSH:
            case Shadower.NINJA_AMBUSH:
            case BlazeWizard.SLOW:
            case BlazeWizard.SEAL:
            case NightWalker.SHADOW_WEB:
            case Crusader.ARMOR_CRASH:
            case DragonKnight.POWER_CRASH:
            case WhiteKnight.MAGIC_CRASH:
            case Priest.DISPEL:
            case SuperGM.HEAL_PLUS_DISPEL:
                return true;
        }
        return false;
    }

    private boolean isPartyBuff() {
        if (lt == null || rb == null) {
            return false;
        }
        return !((sourceid >= 1211003 && sourceid <= 1211008) || sourceid == Paladin.HOLY_CHARGE_SWORD || sourceid == Paladin.DIVINE_CHARGE_BW || sourceid == DawnWarrior.SOUL_CHARGE);
    }

    private boolean isHeal() {
        return sourceid == Cleric.HEAL || sourceid == SuperGM.HEAL_PLUS_DISPEL;
    }

    private boolean isResurrection() {
        return sourceid == Bishop.RESURRECTION || sourceid == SuperGM.RESURRECTION;
    }

    private boolean isTimeLeap() {
        return sourceid == Buccaneer.TIME_LEAP;
    }

    public boolean isDragonBlood() {
        return skill && sourceid == DragonKnight.DRAGON_BLOOD;
    }

    public boolean isBerserk() {
        return skill && sourceid == DarkKnight.BERSERK;
    }

    public boolean isRecovery() {
        return sourceid == Beginner.RECOVERY || sourceid == Noblesse.RECOVERY || sourceid == Legend.RECOVERY;
    }

    private boolean isDs() {
        return skill && (sourceid == Rogue.DARK_SIGHT || sourceid == WindArcher.WIND_WALK || sourceid == NightWalker.DARK_SIGHT);
    }

    private boolean isCombo() {
        return skill && (sourceid == Crusader.COMBO_ATTACK || sourceid == DawnWarrior.COMBO_ATTACK);
    }

    private boolean isEnrage() {
        return skill && sourceid == Hero.ENRAGE;
    }

    public boolean isBeholder() {
        return skill && sourceid == DarkKnight.BEHOLDER;
    }

    private boolean isShadowPartner() {
        return skill && (sourceid == Hermit.SHADOW_PARTNER || sourceid == NightWalker.SHADOW_PARTNER);
    }

    private boolean isChakra() {
        return skill && sourceid == ChiefBandit.CHAKRA;
    }

    public boolean isMonsterRiding() {
        return skill && (sourceid % 10000000 == 1004 || sourceid == Corsair.BATTLESHIP || sourceid == Beginner.SPACESHIP || sourceid == Noblesse.SPACESHIP || sourceid == Beginner.YETI_RIDER || sourceid == Beginner.YETI_MOUNT || sourceid == Beginner.WITCH_BROOMSTICK || sourceid == Beginner.BARLOG_MOUNT || sourceid == Noblesse.YETI_RIDER || sourceid == Noblesse.YETI_MOUNT || sourceid == Noblesse.WITCH_BROOMSTICK || sourceid == Noblesse.BARLOG_MOUNT || sourceid == Legend.YETI_RIDER || sourceid == Legend.YETI_MOUNT || sourceid == Legend.WITCH_BROOMSTICK || sourceid == Legend.BARLOG_MOUNT);
    }

    public boolean isMagicDoor() {
        return skill && sourceid == Priest.MYSTIC_DOOR;
    }

    public boolean isPoison() {
        return skill && (sourceid == FPMage.POISON_MIST || sourceid == FPWizard.POISON_BREATH || sourceid == FPMage.ELEMENT_COMPOSITION || sourceid == NightWalker.POISON_BOMB || sourceid == BlazeWizard.FLAME_GEAR);
    }

    public boolean isMorph() {
        return morphId > 0;
    }

    public boolean isMorphWithoutAttack() {
        return morphId > 0 && morphId < 100; // Every morph item I have found has been under 100, pirate skill transforms start at 1000.
    }

    private boolean isMist() {
        return skill && (sourceid == FPMage.POISON_MIST || sourceid == Shadower.SMOKESCREEN || sourceid == BlazeWizard.FLAME_GEAR || sourceid == NightWalker.POISON_BOMB || sourceid == Evan.RECOVERY_AURA);
    }

    private boolean isSoulArrow() {
        return skill && (sourceid == Hunter.SOUL_ARROW_BOW || sourceid == Crossbowman.SOUL_ARROW || sourceid == WindArcher.SOUL_ARROW);
    }

    private boolean isShadowClaw() {
        return skill && sourceid == NightLord.SHADOW_STARS;
    }

    private boolean isCrash() {
        return skill && (sourceid == DragonKnight.POWER_CRASH || sourceid == Crusader.ARMOR_CRASH || sourceid == WhiteKnight.MAGIC_CRASH);
    }

    private boolean isDispel() {
        return skill && (sourceid == Priest.DISPEL || sourceid == SuperGM.HEAL_PLUS_DISPEL);
    }

    private boolean isHeroWill() {
        if (skill) {
            switch (sourceid) {
                case Hero.HEROS_WILL:
                case Paladin.HEROS_WILL:
                case DarkKnight.HEROS_WILL:
                case FPArchMage.HEROS_WILL:
                case ILArchMage.HEROS_WILL:
                case Bishop.HEROS_WILL:
                case Bowmaster.HEROS_WILL:
                case Marksman.HEROS_WILL:
                case NightLord.HEROS_WILL:
                case Shadower.HEROS_WILL:
                case Buccaneer.PIRATES_RAGE:
                case Aran.HEROS_WILL:
                    return true;
                default:
                    return false;
            }
        }
        return false;
    }

    private boolean isDash() {
        return skill && (sourceid == Pirate.DASH || sourceid == ThunderBreaker.DASH || sourceid == Beginner.SPACE_DASH || sourceid == Noblesse.SPACE_DASH);
    }

    private boolean isSkillMorph() {
        return skill && (sourceid == Buccaneer.SUPER_TRANSFORMATION || sourceid == Marauder.TRANSFORMATION || sourceid == WindArcher.EAGLE_EYE || sourceid == ThunderBreaker.TRANSFORMATION);
    }

    private boolean isInfusion() {
        return skill && (sourceid == Buccaneer.SPEED_INFUSION || sourceid == Corsair.SPEED_INFUSION || sourceid == ThunderBreaker.SPEED_INFUSION);
    }

    private boolean isCygnusFA() {
        return skill && (sourceid == DawnWarrior.FINAL_ATTACK || sourceid == WindArcher.FINAL_ATTACK);
    }

    private boolean isComboReset() {
        return sourceid == Aran.COMBO_BARRIER || sourceid == Aran.COMBO_DRAIN;
    }

    private int getFatigue() {
        return fatigue;
    }

    private int getMorph() {
        return morphId;
    }

    private int getMorph(MapleCharacter chr) {
        if (morphId % 10 == 0) {
            return morphId + chr.getGender();
        }
        return morphId + 100 * chr.getGender();
    }

    private SummonMovementType getSummonMovementType() {
        if (!skill) {
            return null;
        }
        switch (sourceid) {
            case Ranger.PUPPET:
            case Sniper.PUPPET:
            case WindArcher.PUPPET:
            case Outlaw.OCTOPUS:
            case Corsair.WRATH_OF_THE_OCTOPI:
                return SummonMovementType.STATIONARY;
            case Ranger.SILVER_HAWK:
            case Sniper.GOLDEN_EAGLE:
            case Priest.SUMMON_DRAGON:
            case Marksman.FROSTPREY:
            case Bowmaster.PHOENIX:
            case Outlaw.GAVIOTA:
                return SummonMovementType.CIRCLE_FOLLOW;
            case DarkKnight.BEHOLDER:
            case FPArchMage.ELQUINES:
            case ILArchMage.IFRIT:
            case Bishop.BAHAMUT:
            case DawnWarrior.SOUL:
            case BlazeWizard.FLAME:
            case BlazeWizard.IFRIT:
            case WindArcher.STORM:
            case NightWalker.DARKNESS:
            case ThunderBreaker.LIGHTNING:
                return SummonMovementType.FOLLOW;
        }
        return null;
    }


    public boolean hasNoIcon() {
        return (sourceid == 3111002 || sourceid == 3211002 || + // puppet, puppet
                sourceid == 3211005 || sourceid == 2311002 || + // golden eagle, mystic door
                sourceid == 2121005 || sourceid == 2221005 || + // elquines, ifrit
                sourceid == 2321003 || sourceid == 3121006 || + // bahamut, phoenix
                sourceid == 3221005 || sourceid == 3111005 || + // frostprey, silver hawk
                sourceid == 2311006 || sourceid == 5220002 || + // summon dragon, wrath of the octopi
                sourceid == 5211001 || sourceid == 5211002); // octopus, gaviota
    }

    public boolean isSkill() {
        return skill;
    }

    public int getSourceId() {
        return sourceid;
    }

    public boolean makeChanceResult() {
        return prop == 1.0 || Math.random() < prop;
    }

    private static class CancelEffectAction implements Runnable {

        private MapleStatEffect effect;
        private WeakReference<MapleCharacter> target;
        private long startTime;

        public CancelEffectAction(MapleCharacter target, MapleStatEffect effect, long startTime) {
            this.effect = effect;
            this.target = new WeakReference<>(target);
            this.startTime = startTime;
        }

        @Override
        public void run() {
            MapleCharacter realTarget = target.get();
            if (realTarget != null) {
                realTarget.cancelEffect(effect, false, startTime);
            }
        }
    }

    public short getHp() {
        return hp;
    }

    public short getMp() {
        return mp;
    }

    public short getHpCon() {
        return hpCon;
    }

    public short getMpCon() {
        return mpCon;
    }

    public short getMatk() {
        return matk;
    }

    public short getWatk() {
        return watk;
    }

    public int getDuration() {
        return duration;
    }

    public List<Pair<MapleBuffStat, Integer>> getStatups() {
        return statups;
    }

    public boolean sameSource(MapleStatEffect effect) {
        return this.sourceid == effect.sourceid && this.skill == effect.skill;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getDamage() {
        return damage;
    }

    public int getAttackCount() {
        return attackCount;
    }

    public int getMobCount() {
        return mobCount;
    }

    public int getFixDamage() {
        return fixdamage;
    }

    public byte getBulletCount() {
        return bulletCount;
    }

    public byte getBulletConsume() {
        return bulletConsume;
    }

    public int getMoneyCon() {
        return moneyCon;
    }

    public int getCooldown() {
        return cooldown;
    }

    public Map<MonsterStatus, Integer> getMonsterStati() {
        return monsterStatus;
    }
}