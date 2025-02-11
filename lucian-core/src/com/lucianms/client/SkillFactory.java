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
package com.lucianms.client;

import com.lucianms.constants.skills.*;
import com.lucianms.server.MapleStatEffect;
import com.lucianms.server.life.Element;
import org.apache.commons.io.FilenameUtils;
import provider.MapleData;
import provider.MapleDataFileEntry;
import provider.MapleDataProvider;
import provider.MapleDataTool;
import provider.tools.SkillDataProvider;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SkillFactory {

    private static Map<Integer, Skill> SKILLS_CACHE = new HashMap<>();

    public static Skill getSkill(int id) {
        return SKILLS_CACHE.get(id);
    }

    public static Map<Integer, Skill> getSkills() {
        return Collections.unmodifiableMap(SKILLS_CACHE);
    }

    public static void createCache() {
        SKILLS_CACHE.clear();

        MapleDataProvider WZ = SkillDataProvider.getProvider();
        for (MapleDataFileEntry file : WZ.getRoot().getFiles()) {
            String baseName = FilenameUtils.getBaseName(file.getName());
            int jobID;
            try {
                jobID = Integer.parseInt(baseName);
            } catch (NumberFormatException ignore) {
                continue;
            }
            MapleData data = SkillDataProvider.getJob(jobID);
            if (data != null && (data = data.getChildByPath("skill")) != null) {
                for (MapleData skills : data.getChildren()) {
                    int skillID = Integer.parseInt(skills.getName());
                    Skill skill = loadFromData(skillID, skills);
                    SKILLS_CACHE.put(skillID, skill);
                }
            }
        }
        System.gc();
    }

    private static Skill loadFromData(int id, MapleData data) {
        Skill ret = new Skill(id);
        ret.weapon = MapleDataTool.getInt("weapon", data, 0);
        ret.hidden = MapleDataTool.getInt("invisible", data, 0) == 1;
        String elem = MapleDataTool.getString("elemAttr", data, null);
        ret.element = (elem == null) ? Element.NEUTRAL : Element.getFromChar(elem.charAt(0));
        MapleData effect = data.getChildByPath("effect");

        boolean isBuff;
        int skillType = MapleDataTool.getInt("skillType", data, -1);
        if (skillType == 2) {
            isBuff = true;
        } else {
            MapleData actionData = data.getChildByPath("action");
            boolean isAction = false;
            if (actionData == null) {
                if (data.getChildByPath("prepare/action") == null) {
                    isAction = true;
                } else {
                    switch (id) {
                        case Gunslinger.INVISIBLE_SHOT:
                        case Corsair.HYPNOTIZE:
                            isAction = true;
                            break;
                    }
                }
            } else {
                isAction = true;
            }
            ret.action = isAction;
            MapleData hit = data.getChildByPath("hit");
            MapleData ball = data.getChildByPath("ball");
            isBuff = effect != null && hit == null && ball == null;
            isBuff |= actionData != null && MapleDataTool.getString("0", actionData, "").equals("alert2");
            switch (id) {
                case Hero.RUSH:
                case Paladin.RUSH:
                case DarkKnight.RUSH:
                case Hero.MONSTER_MAGNET:
                case Paladin.MONSTER_MAGNET:
                case DarkKnight.MONSTER_MAGNET:
                case DragonKnight.SACRIFICE:
                case FPMage.EXPLOSION:
                case FPMage.POISON_MIST:
                case Cleric.HEAL:
                case Ranger.MORTAL_BLOW:
                case Sniper.MORTAL_BLOW:
                case Assassin.DRAIN:
                case Hermit.SHADOW_WEB:
                case Bandit.STEAL:
                case Shadower.SMOKESCREEN:
                case SuperGM.HEAL_PLUS_DISPEL:
                case Evan.ICE_BREATH:
                case Evan.FIRE_BREATH:
                case Evan.RECOVERY_AURA:
                case Gunslinger.RECOIL_SHOT:
                case Marauder.ENERGY_DRAIN:
                case BlazeWizard.FLAME_GEAR:
                case NightWalker.SHADOW_WEB:
                case NightWalker.POISON_BOMB:
                case NightWalker.VAMPIRE:
                case ChiefBandit.CHAKRA:
                    isBuff = false;
                    break;
                case Beginner.RECOVERY:
                case Beginner.NIMBLE_FEET:
                case Beginner.MONSTER_RIDER:
                case Beginner.ECHO_OF_HERO:
                case Swordsman.IRON_BODY:
                case Fighter.AXE_BOOSTER:
                case Fighter.POWER_GUARD:
                case Fighter.RAGE:
                case Fighter.SWORD_BOOSTER:
                case Crusader.ARMOR_CRASH:
                case Crusader.COMBO_ATTACK:
                case Hero.ENRAGE:
                case Hero.HEROS_WILL:
                case Hero.MAPLE_WARRIOR:
                case Hero.POWER_STANCE:
                case Page.BW_BOOSTER:
                case Page.POWER_GUARD:
                case Page.SWORD_BOOSTER:
                case Page.THREATEN:
                case WhiteKnight.FLAME_CHARGE_BW:
                case WhiteKnight.BLIZZARD_CHARGE_BW:
                case WhiteKnight.LIGHTNING_CHARGE_BW:
                case WhiteKnight.MAGIC_CRASH:
                case WhiteKnight.FIRE_CHARGE_SWORD:
                case WhiteKnight.ICE_CHARGE_SWORD:
                case WhiteKnight.THUNDER_CHARGE_SWORD:
                case Paladin.DIVINE_CHARGE_BW:
                case Paladin.HEROS_WILL:
                case Paladin.MAPLE_WARRIOR:
                case Paladin.POWER_STANCE:
                case Paladin.HOLY_CHARGE_SWORD:
                case Spearman.HYPER_BODY:
                case Spearman.IRON_WILL:
                case Spearman.POLE_ARM_BOOSTER:
                case Spearman.SPEAR_BOOSTER:
                case DragonKnight.DRAGON_BLOOD:
                case DragonKnight.POWER_CRASH:
                case DarkKnight.AURA_OF_THE_BEHOLDER:
                case DarkKnight.BEHOLDER:
                case DarkKnight.HEROS_WILL:
                case DarkKnight.HEX_OF_THE_BEHOLDER:
                case DarkKnight.MAPLE_WARRIOR:
                case DarkKnight.POWER_STANCE:
                case Magician.MAGIC_GUARD:
                case Magician.MAGIC_ARMOR:
                case FPWizard.MEDITATION:
                case FPWizard.SLOW:
                case FPMage.SEAL:
                case FPMage.SPELL_BOOSTER:
                case FPArchMage.HEROS_WILL:
                case FPArchMage.INFINITY:
                case FPArchMage.MANA_REFLECTION:
                case FPArchMage.MAPLE_WARRIOR:
                case ILWizard.MEDITATION:
                case ILMage.SEAL:
                case ILWizard.SLOW:
                case ILMage.SPELL_BOOSTER:
                case ILArchMage.HEROS_WILL:
                case ILArchMage.INFINITY:
                case ILArchMage.MANA_REFLECTION:
                case ILArchMage.MAPLE_WARRIOR:
                case Cleric.INVINCIBLE:
                case Cleric.BLESS:
                case Priest.DISPEL:
                case Priest.DOOM:
                case Priest.HOLY_SYMBOL:
                case Priest.MYSTIC_DOOR:
                case Bishop.HEROS_WILL:
                case Bishop.HOLY_SHIELD:
                case Bishop.INFINITY:
                case Bishop.MANA_REFLECTION:
                case Bishop.MAPLE_WARRIOR:
                case Archer.FOCUS:
                case Hunter.BOW_BOOSTER:
                case Hunter.SOUL_ARROW_BOW:
                case Ranger.PUPPET:
                case Bowmaster.CONCENTRATE:
                case Bowmaster.HEROS_WILL:
                case Bowmaster.MAPLE_WARRIOR:
                case Bowmaster.SHARP_EYES:
                case Crossbowman.CROSSBOW_BOOSTER:
                case Crossbowman.SOUL_ARROW:
                case Sniper.PUPPET:
                case Marksman.BLIND:
                case Marksman.HEROS_WILL:
                case Marksman.MAPLE_WARRIOR:
                case Marksman.SHARP_EYES:
                case Rogue.DARK_SIGHT:
                case Assassin.CLAW_BOOSTER:
                case Assassin.HASTE:
                case Hermit.MESO_UP:
                case Hermit.SHADOW_PARTNER:
                case NightLord.HEROS_WILL:
                case NightLord.MAPLE_WARRIOR:
                case NightLord.NINJA_AMBUSH:
                case NightLord.SHADOW_STARS:
                case Bandit.DAGGER_BOOSTER:
                case Bandit.HASTE:
                case ChiefBandit.MESO_GUARD:
                case ChiefBandit.PICK_POCKET:
                case Shadower.HEROS_WILL:
                case Shadower.MAPLE_WARRIOR:
                case Shadower.NINJA_AMBUSH:
                case Pirate.DASH:
                case Marauder.TRANSFORMATION:
                case Buccaneer.SUPER_TRANSFORMATION:
                case Corsair.BATTLESHIP:
                case SuperGM.HASTE_SUPER:
                case SuperGM.HOLY_SYMBOL:
                case SuperGM.BLESS:
                case SuperGM.HIDE:
                case SuperGM.HYPER_BODY:
                case Noblesse.BLESSING_OF_THE_FAIRY:
                case Noblesse.ECHO_OF_HERO:
                case Noblesse.MONSTER_RIDER:
                case Noblesse.NIMBLE_FEET:
                case Noblesse.RECOVERY:
                case DawnWarrior.COMBO_ATTACK:
                case DawnWarrior.FINAL_ATTACK:
                case DawnWarrior.IRON_BODY:
                case DawnWarrior.RAGE:
                case DawnWarrior.SOUL:
                case DawnWarrior.SOUL_CHARGE:
                case DawnWarrior.SWORD_BOOSTER:
                case BlazeWizard.ELEMENTAL_RESET:
                case BlazeWizard.FLAME:
                case BlazeWizard.IFRIT:
                case BlazeWizard.MAGIC_ARMOR:
                case BlazeWizard.MAGIC_GUARD:
                case BlazeWizard.MEDITATION:
                case BlazeWizard.SEAL:
                case BlazeWizard.SLOW:
                case BlazeWizard.SPELL_BOOSTER:
                case WindArcher.BOW_BOOSTER:
                case WindArcher.EAGLE_EYE:
                case WindArcher.FINAL_ATTACK:
                case WindArcher.FOCUS:
                case WindArcher.PUPPET:
                case WindArcher.SOUL_ARROW:
                case WindArcher.STORM:
                case WindArcher.WIND_WALK:
                case NightWalker.CLAW_BOOSTER:
                case NightWalker.DARKNESS:
                case NightWalker.DARK_SIGHT:
                case NightWalker.HASTE:
                case NightWalker.SHADOW_PARTNER:
                case ThunderBreaker.DASH:
                case ThunderBreaker.ENERGY_CHARGE:
                case ThunderBreaker.ENERGY_DRAIN:
                case ThunderBreaker.KNUCKLE_BOOSTER:
                case ThunderBreaker.LIGHTNING:
                case ThunderBreaker.SPARK:
                case ThunderBreaker.LIGHTNING_CHARGE:
                case ThunderBreaker.SPEED_INFUSION:
                case ThunderBreaker.TRANSFORMATION:
                case Legend.BLESSING_OF_THE_FAIRY:
                case Legend.AGILE_BODY:
                case Legend.ECHO_OF_HERO:
                case Legend.RECOVERY:
                case Legend.MONSTER_RIDER:
                case Aran.MAPLE_WARRIOR:
                case Aran.HEROS_WILL:
                case Aran.POLEARM_BOOSTER:
                case Aran.COMBO_DRAIN:
                case Aran.SNOW_CHARGE:
                case Aran.BODY_PRESSURE:
                case Aran.SMART_KNOCKBACK:
                case Aran.COMBO_BARRIER:
                case Aran.COMBO_ABILITY:
                case Evan.BLESSING_OF_THE_FAIRY:
                case Evan.RECOVERY:
                case Evan.NIMBLE_FEET:
                case Evan.HEROS_WILL:
                case Evan.ECHO_OF_HERO:
                case Evan.MAGIC_BOOSTER:
                case Evan.MAGIC_GUARD:
                case Evan.ELEMENTAL_RESET:
                case Evan.MAPLE_WARRIOR:
                case Evan.MAGIC_RESISTANCE:
                case Evan.MAGIC_SHIELD:
                case Evan.SLOW:
                    isBuff = true;
                    break;
            }
        }
        for (MapleData level : data.getChildByPath("level")) {
            ret.effects.add(MapleStatEffect.loadSkillEffectFromData(level, id, isBuff));
        }
        ret.animationTime = 0;
        if (effect != null) {
            for (MapleData effectEntry : effect) {
                ret.animationTime += MapleDataTool.getIntConvert("delay", effectEntry, 0);
            }
        }
        return ret;
    }
}
