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
package com.lucianms.server.life;

import com.lucianms.server.life.MapleLifeFactory.BanishInfo;
import com.lucianms.server.life.MapleLifeFactory.LoseItem;
import com.lucianms.server.life.MapleLifeFactory.SelfDestruction;
import tools.Pair;

import java.util.*;

/**
 * @author Frz
 */
public class MapleMonsterStats {

    private int exp, hp, mp, level, PADamage, PDDamage, MADamage, MDDamage, dropPeriod, cp, buffToGive, removeAfter;
    private boolean boss, undead, ffaLoot, isExplosiveReward, firstAttack, removeOnMiss;
    private String name;
    private Map<String, Integer> animationTimes = new HashMap<>();
    private Map<Element, ElementalEffectiveness> resistance = new HashMap<>();
    private ArrayList<Pair<Integer, Integer>> skills = new ArrayList<>();
    private ArrayList<LoseItem> loseItem = null;
    private ArrayList<Integer> revives = new ArrayList<>();
    private byte tagColor, tagBgColor;
    private Pair<Integer, Integer> cool = null;
    private BanishInfo banish = null;
    private SelfDestruction selfDestruction = null;
    private boolean friendly;

    public MapleMonsterStats() {
    }

    /**
     * Create a copy of a monster's stats
     *
     * @param stats the stats to copy
     */
    public MapleMonsterStats(MapleMonsterStats stats) {
        name = stats.name;
        tagColor = stats.tagColor;
        tagBgColor = stats.tagBgColor;

        //region ints
        exp = stats.exp;
        hp = stats.hp;
        mp = stats.mp;
        level = stats.level;
        PADamage = stats.PADamage;
        PDDamage = stats.PDDamage;
        MADamage = stats.MADamage;
        MDDamage = stats.MDDamage;
        dropPeriod = stats.dropPeriod;
        cp = stats.cp;
        buffToGive = stats.buffToGive;
        removeAfter = stats.removeAfter;
        //endregion
        //region booleans
        boss = stats.boss;
        undead = stats.undead;
        ffaLoot = stats.ffaLoot;
        isExplosiveReward = stats.isExplosiveReward;
        firstAttack = stats.firstAttack;
        removeOnMiss = stats.removeOnMiss;
        //endregion
        //region collections
        animationTimes.putAll(stats.animationTimes);
        resistance.putAll(stats.resistance);
        skills.addAll(stats.skills);
        revives.addAll(stats.revives);
        if (stats.loseItem != null) {
            loseItem = new ArrayList<>();
            loseItem.addAll(stats.loseItem);
        }
        //endregion

        cool = stats.cool;
        banish = stats.banish;
        selfDestruction = stats.selfDestruction;
        friendly = stats.friendly;
    }

    public int getExp() {
        return exp;
    }

    public void setExp(int exp) {
        this.exp = exp;
    }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public int getMp() {
        return mp;
    }

    public void setMp(int mp) {
        this.mp = mp;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getRemoveAfter() {
        return removeAfter;
    }

    public void setRemoveAfter(int removeAfter) {
        this.removeAfter = removeAfter;
    }

    public int getDropPeriod() {
        return dropPeriod;
    }

    public void setDropPeriod(int dropPeriod) {
        this.dropPeriod = dropPeriod;
    }

    public void setBoss(boolean boss) {
        this.boss = boss;
    }

    public boolean isBoss() {
        return boss;
    }

    public void setFfaLoot(boolean ffaLoot) {
        this.ffaLoot = ffaLoot;
    }

    public boolean isFfaLoot() {
        return ffaLoot;
    }

    public void setAnimationTime(String name, int delay) {
        animationTimes.put(name, delay);
    }

    public int getAnimationTime(String name) {
        Integer ret = animationTimes.get(name);
        if (ret == null) {
            return 500;
        }
        return ret;
    }

    public boolean isMobile() {
        return animationTimes.containsKey("move") || animationTimes.containsKey("fly");
    }

    public ArrayList<Integer> getRevives() {
        return revives;
    }

    public void setRevives(ArrayList<Integer> revives) {
        this.revives = revives;
    }

    public void setUndead(boolean undead) {
        this.undead = undead;
    }

    public boolean getUndead() {
        return undead;
    }

    public void setEffectiveness(Element e, ElementalEffectiveness ee) {
        resistance.put(e, ee);
    }

    public ElementalEffectiveness getEffectiveness(Element e) {
        ElementalEffectiveness elementalEffectiveness = resistance.get(e);
        if (elementalEffectiveness == null) {
            return ElementalEffectiveness.NORMAL;
        } else {
            return elementalEffectiveness;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte getTagColor() {
        return tagColor;
    }

    public void setTagColor(int tagColor) {
        this.tagColor = (byte) tagColor;
    }

    public byte getTagBgColor() {
        return tagBgColor;
    }

    public void setTagBgColor(int tagBgColor) {
        this.tagBgColor = (byte) tagBgColor;
    }

    public void setSkills(List<Pair<Integer, Integer>> skills) {
        for (Pair<Integer, Integer> skill : skills) {
            this.skills.add(skill);
        }
    }

    public List<Pair<Integer, Integer>> getSkills() {
        return Collections.unmodifiableList(this.skills);
    }

    public int getNoSkills() {
        return this.skills.size();
    }

    public boolean hasSkill(int skillId, int level) {
        for (Pair<Integer, Integer> skill : skills) {
            if (skill.getLeft() == skillId && skill.getRight() == level) {
                return true;
            }
        }
        return false;
    }

    public void setFirstAttack(boolean firstAttack) {
        this.firstAttack = firstAttack;
    }

    public boolean isFirstAttack() {
        return firstAttack;
    }

    public void setBuffToGive(int buff) {
        this.buffToGive = buff;
    }

    public int getBuffToGive() {
        return buffToGive;
    }

    void removeEffectiveness(Element e) {
        resistance.remove(e);
    }

    public BanishInfo getBanishInfo() {
        return banish;
    }

    public void setBanishInfo(BanishInfo banish) {
        this.banish = banish;
    }

    public int getPADamage() {
        return PADamage;
    }

    public void setPADamage(int PADamage) {
        this.PADamage = PADamage;
    }

    public int getCP() {
        return cp;
    }

    public void setCP(int cp) {
        this.cp = cp;
    }

    public List<LoseItem> loseItem() {
        return loseItem;
    }

    public void addLoseItem(LoseItem li) {
        if (loseItem == null) {
            loseItem = new ArrayList<>();
        }
        loseItem.add(li);
    }

    public SelfDestruction getSelfDestruction() {
        return selfDestruction;
    }

    public void setSelfDestruction(SelfDestruction sd) {
        this.selfDestruction = sd;
    }

    public void setExplosiveReward(boolean isExplosiveReward) {
        this.isExplosiveReward = isExplosiveReward;
    }

    public boolean isExplosiveReward() {
        return isExplosiveReward;
    }

    public void setRemoveOnMiss(boolean removeOnMiss) {
        this.removeOnMiss = removeOnMiss;
    }

    public boolean removeOnMiss() {
        return removeOnMiss;
    }

    public void setCool(Pair<Integer, Integer> cool) {
        this.cool = cool;
    }

    public Pair<Integer, Integer> getCool() {
        return cool;
    }

    public int getPDDamage() {
        return PDDamage;
    }

    public int getMADamage() {
        return MADamage;
    }

    public int getMDDamage() {
        return MDDamage;
    }

    public boolean isFriendly() {
        return friendly;
    }

    public void setFriendly(boolean value) {
        this.friendly = value;
    }

    public void setPDDamage(int PDDamage) {
        this.PDDamage = PDDamage;
    }

    public void setMADamage(int MADamage) {
        this.MADamage = MADamage;
    }

    public void setMDDamage(int MDDamage) {
        this.MDDamage = MDDamage;
    }
}
