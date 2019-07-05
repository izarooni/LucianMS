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

import com.lucianms.client.*;
import com.lucianms.client.inventory.Equip;
import com.lucianms.client.inventory.MapleInventoryType;
import com.lucianms.client.meta.Occupation;
import com.lucianms.client.status.MonsterStatus;
import com.lucianms.client.status.MonsterStatusEffect;
import com.lucianms.constants.skills.*;
import com.lucianms.cquest.CQuestData;
import com.lucianms.cquest.requirement.CQuestKillRequirement;
import com.lucianms.io.scripting.Achievements;
import com.lucianms.io.scripting.event.EventInstanceManager;
import com.lucianms.scheduler.Task;
import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.server.BuffContainer;
import com.lucianms.server.life.MapleLifeFactory.BanishInfo;
import com.lucianms.server.maps.MapleMap;
import com.lucianms.server.maps.MapleMapObject;
import com.lucianms.server.maps.MapleMapObjectType;
import com.lucianms.server.world.MapleParty;
import tools.Functions;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.Randomizer;

import java.awt.*;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public class MapleMonster extends AbstractLoadedMapleLife {

    private long hp;
    private int mp;
    private int team;
    private int mimicTemplateID;
    private int venomMultiplier;
    private boolean controllerHasAggro;
    private boolean controllerKnowsAboutAggro;
    private MapleMap map;
    private MapleMonsterStats stats;
    private MapleMonsterStats overrideStats;
    private WeakReference<MapleCharacter> controller = new WeakReference<>(null);
    private EventInstanceManager eventInstance;
    private ArrayList<Integer> stolenItems = new ArrayList<>();
    private ArrayList<Pair<Integer, Integer>> usedSkills = new ArrayList<>();
    private ArrayList<MonsterStatus> alreadyBuffed = new ArrayList<>();
    private HashSet<MonsterListener> listeners = new HashSet<>();
    private Map<Pair<Integer, Integer>, Integer> skillsUsed = new HashMap<>();
    private EnumMap<MonsterStatus, MonsterStatusEffect> stati = new EnumMap<>(MonsterStatus.class);
    private final HashMap<Integer, AtomicLong> takenDamage = new HashMap<>();
    private boolean fake;
    private boolean dropsDisabled;
    private Task damageTask;

    public ReentrantLock monsterLock = new ReentrantLock();

    public MapleMonster(int id, MapleMonsterStats stats) {
        super(id);
        initWithStats(stats);
    }

    public MapleMonster(MapleMonster monster) {
        super(monster);
        initWithStats(monster.stats);
    }

    private void initWithStats(MapleMonsterStats stats) {
        setStance(5);
        this.stats = stats;
        hp = stats.getHp();
        mp = stats.getMp();
    }

    public void setOverrideStats(MapleMonsterStats overrideStats) {
        this.overrideStats = overrideStats;
        hp = overrideStats.getHp();
        mp = overrideStats.getMp();
    }

    public void disableDrops() {
        this.dropsDisabled = true;
    }

    public boolean dropsDisabled() {
        return dropsDisabled;
    }

    public Task getDamageTask() {
        return damageTask;
    }

    public void setDamageTask(Task damageTask) {
        this.damageTask = damageTask;
    }

    public void setMap(MapleMap map) {
        this.map = map;
    }

    public long getHp() {
        return hp;
    }

    public void setHp(long hp) {
        this.hp = hp;
    }

    public long getMaxHp() {
        if (overrideStats != null && overrideStats.getHp() > 0) {
            return overrideStats.getHp();
        }
        return stats.getHp();
    }

    public int getMp() {
        return mp;
    }

    public void setMp(int mp) {
        if (mp < 0) {
            mp = 0;
        }
        this.mp = mp;
    }

    public int getMaxMp() {
        if (overrideStats != null) {
            return overrideStats.getMp();
        }
        return stats.getMp();
    }

    public int getExp() {
        if (overrideStats != null) {
            return overrideStats.getExp();
        }
        return stats.getExp();
    }

    public int getLevel() {
        return stats.getLevel();
    }

    public void setLevel(int level) {
        stats.setLevel(level);
    }

    public int getCP() {
        return stats.getCP();
    }

    public int getTeam() {
        return team;
    }

    public void setTeam(int team) {
        this.team = team;
    }

    public int getMimicTemplateID() {
        return mimicTemplateID;
    }

    public void setMimicTemplateID(int mimicTemplateID) {
        this.mimicTemplateID = mimicTemplateID;
    }

    public int getVenomMulti() {
        return this.venomMultiplier;
    }

    public void setVenomMulti(int multiplier) {
        this.venomMultiplier = multiplier;
    }

    public int getSummonEffect() {
        return getStats().getSummonEffect();
    }

    public MapleMonsterStats getStats() {
        return overrideStats == null ? stats : overrideStats;
    }

    public boolean isBoss() {
        return stats.isBoss() || isHT();
    }

    public int getAnimationTime(String name) {
        return stats.getAnimationTime(name);
    }

    private List<Integer> getRevives() {
        if (overrideStats != null) {
            return overrideStats.getRevives();
        }
        return stats.getRevives();
    }

    private byte getTagColor() {
        if (overrideStats != null) {
            return overrideStats.getTagColor();
        }
        return stats.getTagColor();
    }

    private byte getTagBgColor() {
        if (overrideStats != null) {
            return overrideStats.getTagBgColor();
        }
        return stats.getTagBgColor();
    }

    public synchronized void damage(MapleCharacter from, long damage) {
        if (!isAlive()) {
            return;
        }
        hp -= damage;
        takenDamage.computeIfAbsent(from.getId(), id -> new AtomicLong()).addAndGet(damage);

        if (hasBossHPBar()) {
            from.getMap().sendPacket(makeBossHPBarPacket());
        } else if (!isBoss()) {
            int remainingHP = (int) Math.max(0, hp * 100f / getMaxHp());
            byte[] packet = MaplePacketCreator.showMonsterHP(getObjectId(), remainingHP);
            MapleParty party = from.getParty();
            if (party != null) {
                for (MapleCharacter player : party.getPlayers()) {
                    if (player.getMap() == from.getMap()) {
                        player.announce(packet);
                    }
                }
            } else {
                from.announce(packet);
            }
        }
    }

    public void heal(int hp, int mp) {
        long hp2Heal = getHp() + hp;
        int mp2Heal = getMp() + mp;
        if (hp2Heal >= getMaxHp()) {
            hp2Heal = getMaxHp();
        }
        if (mp2Heal >= getMaxMp()) {
            mp2Heal = getMaxMp();
        }
        setHp(hp2Heal);
        setMp(mp2Heal);
        getMap().broadcastMessage(MaplePacketCreator.healMonster(getObjectId(), hp));
    }

    public boolean isAttackedBy(MapleCharacter chr) {
        return takenDamage.containsKey(chr.getId());
    }

    private void distributeExperienceToParty(int partyID, long exp, int kPlayerID) {
        ArrayList<MapleCharacter> members = new ArrayList<>();
        map.forEachPlayer(members::add, p -> p.getPartyID() == partyID);

        final int reqLevel = getLevel() - 5;
        int minLeechLevel = 0;
        int partyLevelSum = 0;
        int leechCount = 0;

        for (MapleCharacter player : members) {
            if (player.getLevel() >= reqLevel) {
                minLeechLevel = Math.min(player.getLevel() - 5, reqLevel);
            }
        }

        for (MapleCharacter mc : members) {
            if (mc.getLevel() >= minLeechLevel) {
                partyLevelSum += mc.getLevel();
                leechCount++;
            }
        }

        final int highestAttackerID = getHighestAttackerID();

        for (MapleCharacter member : members) {
            int id = member.getId();
            int level = member.getLevel();
            if (level >= minLeechLevel) {
                boolean isKiller = kPlayerID == id;
                boolean mostDamage = highestAttackerID == id;
                double gainExp = (exp * 0.8f) * (level / (double) partyLevelSum);
                gainExp += gainExp * Math.log(leechCount);
                if (mostDamage) {
                    gainExp += (exp * 0.6f);
                }
                giveExpToCharacter(member, (int) (gainExp / leechCount), isKiller, leechCount);
            }
        }
        members.clear();
    }

    public void distributeExperience(int killerId) {
        if (isAlive() || killerId == 0) {
            return;
        }
        final int exp = getExp();
        final long maxHP = getMaxHp();

        Map<Integer, Long> partyDist = new HashMap<>();
        for (Entry<Integer, AtomicLong> entry : takenDamage.entrySet()) {
            MapleCharacter player = getMap().getCharacterById(entry.getKey());
            if (player != null) {
                boolean isKiller = player.getId() == killerId;
                AtomicLong atomicDamage = entry.getValue();
                float portionedExp = Math.min(maxHP, atomicDamage.get()) / maxHP;
                if (isKiller && takenDamage.size() > 1) {
                    portionedExp += 0.05;
                }
                int gainExp = (int) (exp * (portionedExp * 100f));

                MapleParty party = player.getParty();
                if (party != null) {
                    for (MapleCharacter members : party.getPlayers()) {
                        if (members.getMap() == getMap()) {
                            partyDist.compute(party.getID(), (id, inc) -> (inc == null ? 0 : inc) + gainExp);
                        }
                    }
                } else {
                    giveExpToCharacter(player, gainExp, isKiller, 0);
                }
            }
        }
        for (Entry<Integer, Long> party : partyDist.entrySet()) {
            distributeExperienceToParty(party.getKey(), party.getValue(), killerId);
        }
        partyDist.clear();
    }

    public void giveExpToCharacter(MapleCharacter attacker, long exp, boolean isKiller, int shareCount) {
        if (exp < 1) return;
        if (isKiller) {
            if (eventInstance != null) {
                eventInstance.monsterKilled(attacker, this);
            }
        }

        int partyBonus = 0;

        if (attacker.isAlive()) {
            long personalExp = exp * attacker.getExpRate();
            if (shareCount > 1) {
                float bonusModifier = (110 + (5 * (shareCount - 2)));
                partyBonus = (int) (personalExp * (bonusModifier / 1000));
            }

            Integer holySymbol = attacker.getBuffedValue(MapleBuffStat.HOLY_SYMBOL);
            if (holySymbol != null) {
                BuffContainer container = attacker.getEffects().get(MapleBuffStat.HOLY_SYMBOL);
                if (shareCount == 0 && container.getSourceID() != SuperGM.HOLY_SYMBOL) {
                    personalExp *= 1.0 + (holySymbol.doubleValue() / 500.0);
                } else {
                    personalExp *= 1.0 + (holySymbol.doubleValue() / 100.0);
                }
            }
            if (stati.containsKey(MonsterStatus.SHOWDOWN)) {
                personalExp *= (stati.get(MonsterStatus.SHOWDOWN).getStati().get(MonsterStatus.SHOWDOWN).doubleValue() / 100.0 + 1.0);
            }
            attacker.gainExp(personalExp, partyBonus, true, false, isKiller);
            attacker.mobKilled(getId());
            attacker.increaseEquipExp((int) Math.min(Integer.MAX_VALUE, personalExp));
        }
    }

    public MapleCharacter killBy(MapleCharacter killer) {
        distributeExperience(killer != null ? killer.getId() : 0);

        if (getController() != null) { // this can/should only happen when a hidden gm attacks the monster
            getController().getClient().announce(MaplePacketCreator.stopControllingMonster(this.getObjectId()));
            getController().stopControllingMonster(this);
        }

        final List<Integer> toSpawn = getRevives(); // this doesn't work (?)
        if (toSpawn != null) {
            if (toSpawn.contains(9300216) && getMap().getId() > 925000000 && getMap().getId() < 926000000) {
                getMap().broadcastMessage(MaplePacketCreator.playSound("Dojang/clear"));
                getMap().broadcastMessage(MaplePacketCreator.showEffect("dojang/end/clear"));
            }

            Pair<Integer, String> timeMob = getMap().getTimeMob();
            if (timeMob != null) {
                if (toSpawn.contains(timeMob.getLeft())) {
                    getMap().broadcastMessage(MaplePacketCreator.serverNotice(6, timeMob.getRight()));
                }

                if (timeMob.getLeft() == 9300338 && (getMap().getId() >= 922240100 && getMap().getId() <= 922240119)) {
                    if (!getMap().containsNPC(9001108)) {
                        MapleNPC npc = MapleLifeFactory.getNPC(9001108);
                        npc.setPosition(new Point(172, 9));
                        npc.setCy(9);
                        npc.setRx0(172 + 50);
                        npc.setRx1(172 - 50);
                        npc.setFh(27);
                        getMap().addMapObject(npc);
                        getMap().broadcastMessage(MaplePacketCreator.spawnNPC(npc));
                    } else {
                        getMap().toggleHiddenNPC(9001108);
                    }
                }
            }
            TaskExecutor.createTask(new Runnable() {
                @Override
                public void run() {
                    for (Integer mid : toSpawn) {
                        final MapleMonster mob = MapleLifeFactory.getMonster(mid);
                        if (mob != null) {
                            mob.setPosition(getPosition());
                            if (dropsDisabled()) {
                                mob.disableDrops();
                            }
                            getMap().spawnMonsterOnGroudBelow(mob, getPosition());
                        }
                    }
                    if (getId() >= 8810002 && getId() <= 8810009) { // horntail defeated
                        for (int i = 8810010; i < 8810018; i++) {
                            MapleMonster ht = getMap().getMonsterById(i);
                            if (ht == null) {
                                return;
                            }
                        }
                        getMap().killMonster(getMap().getMonsterById(8810018), killer, true);
                    }
                }
            }, getAnimationTime("die1"));
        }
        if (eventInstance != null) {
            if (!getStats().isFriendly()) {
                eventInstance.monsterKilled(this);
            }
        }
        listeners.forEach(listener -> listener.monsterKilled(this, killer));
        MapleCharacter looter = map.getCharacterById(getHighestAttackerID());
        if (killer != null) {
            Achievements.testFor(killer, getId());

            Occupation occupation = killer.getOccupation();
            if (occupation != null && occupation.getType() == Occupation.Type.Trainer && isBoss()) {
                if (occupation.gainExperience(3)) {
                    killer.sendMessage("Your occupation is now level {}", occupation.getLevel());
                }
            }

            for (CQuestData data : killer.getCustomQuests().values()) {
                if (!data.isCompleted()) {
                    CQuestKillRequirement toKill = data.getToKill();
                    Pair<Integer, Integer> p = toKill.get(getId());
                    if (p != null && p.right < p.left) { // don't exceed requirement variable
                        toKill.incrementRequirement(getId(), 1); // increment progress
                        if (!data.isSilentComplete()) {
                            killer.announce(MaplePacketCreator.earnTitleMessage(String.format("[%s] Monster killed '%s' [%d / %d]", data.getName(), getName(), p.right, p.left)));
                        }
                        boolean checked = toKill.isFinished(); // store to local variable before updating
                        if (data.checkRequirements() && !checked) { // update checked; if requirement is finished and previously was not...
                            data.announceCompletion(killer.getClient());
                        }
                    }
                }
            }
            Equip weapon = killer.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
            if (weapon != null) {
                weapon.setEliminations(weapon.getEliminations() + 1);
            }
        }

        return looter != null ? looter : killer;
    }

    // should only really be used to determine drop owner
    private int getHighestAttackerID() {
        int curId = 0;
        long curDmg = 0;

        for (Entry<Integer, AtomicLong> damage : takenDamage.entrySet()) {
            curId = damage.getValue().get() >= curDmg ? damage.getKey() : curId;
            curDmg = damage.getKey() == curId ? damage.getValue().get() : curDmg;
        }

        return curId;
    }

    public boolean isAlive() {
        return this.hp > 0;
    }

    public MapleCharacter getController() {
        return controller.get();
    }

    public void setController(MapleCharacter controller) {
        this.controller = new WeakReference<>(controller);
    }

    public void switchController(MapleCharacter newController, boolean immediateAggro) {
        MapleCharacter controllers = getController();
        if (controllers == newController) {
            return;
        }
        if (controllers != null) {
            controllers.stopControllingMonster(this);
            controllers.getClient().announce(MaplePacketCreator.stopControllingMonster(getObjectId()));
        }
        newController.controlMonster(this, immediateAggro);
        setController(newController);
        if (immediateAggro) {
            setControllerHasAggro(true);
        }
        setControllerKnowsAboutAggro(false);
    }

    public HashSet<MonsterListener> getListeners() {
        return listeners;
    }

    public boolean isControllerHasAggro() {
        return !fake && controllerHasAggro;
    }

    public void setControllerHasAggro(boolean controllerHasAggro) {
        if (fake) {
            return;
        }
        this.controllerHasAggro = controllerHasAggro;
    }

    public boolean isControllerKnowsAboutAggro() {
        return !fake && controllerKnowsAboutAggro;
    }

    public void setControllerKnowsAboutAggro(boolean controllerKnowsAboutAggro) {
        if (fake) {
            return;
        }
        this.controllerKnowsAboutAggro = controllerKnowsAboutAggro;
    }

    public byte[] makeBossHPBarPacket() {
        long maxHp = getMaxHp();
        int pp = (int) Math.ceil((100f / maxHp) * hp);
        int templateID = getMimicTemplateID() > 0 ? getMimicTemplateID() : getId();
        return MaplePacketCreator.showBossHP(templateID, Math.min(100, Math.max(0, pp)), 100, getTagColor(), getTagBgColor());
    }

    public boolean hasBossHPBar() {
        return (isBoss() && getTagColor() > 0) || isHT();
    }

    private boolean isHT() {
        return getId() == 8810018;
    }

    @Override
    public void sendSpawnData(MapleClient c) {
        if (!isAlive()) {
            return;
        }
        if (isFake()) {
            c.announce(MaplePacketCreator.spawnFakeMonster(this, 0));
        } else {
            c.announce(MaplePacketCreator.spawnMonster(this, false));
        }
        if (stati.size() > 0) {
            for (final MonsterStatusEffect mse : this.stati.values()) {
                c.announce(MaplePacketCreator.applyMonsterStatus(getObjectId(), mse, null));
            }
        }
        if (hasBossHPBar()) {
            if (this.getMap().countMonster(8810026) > 0 && this.getMap().getId() == 240060200) {
                this.getMap().killAllMonsters();
                return;
            }
            c.announce(makeBossHPBarPacket());
        }
    }

    @Override
    public void sendDestroyData(MapleClient client) {
        /*
        Issue causing monsters that leave the player's viewport to become untargetable.
        Monsters aren't considered ranged entities anyways as every monster in a field is sent to the user's client
        immediately regardless of range between itself and the player avatar.

        Also, this packet does not remove the monster. Using the false parameter does not cause the monster to disappear
        while using TRUE triggers the monsters death animation (not what we want in this case).
         */
//        client.announce(MaplePacketCreator.killMonster(getObjectId(), false));
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.MONSTER;
    }

    public void setEventInstance(EventInstanceManager eventInstance) {
        this.eventInstance = eventInstance;
    }

    public EventInstanceManager getEventInstance() {
        return eventInstance;
    }

    public boolean isMobile() {
        return stats.isMobile();
    }

    public ElementalEffectiveness getEffectiveness(Element e) {
        if (stati.size() > 0 && stati.get(MonsterStatus.DOOM) != null) {
            return ElementalEffectiveness.NORMAL; // like blue snails
        }
        return stats.getEffectiveness(e);
    }

    public void applyDamageOvertime(MapleCharacter from, long duration) {
        MobSkill mSkill = MobSkillFactory.getMobSkill(125, 1);
        mSkill.setDuration(duration);
        Skill skill = SkillFactory.getSkill(4220005); // venom stab
        MonsterStatusEffect statusEffect = new MonsterStatusEffect(Map.of(MonsterStatus.POISON, 1), skill, mSkill, true);
        applyStatus(from, statusEffect, false, duration);

        int calcDamage = (int) (from.calculateMaxBaseDamage(from.getTotalWatk()) * 0.75);
        if (calcDamage > 0) {
            Runnable cancelTask = new Runnable() {
                @Override
                public void run() {
                    Functions.requireNotNull(getDamageTask(), Task::cancel);
                    setDamageTask(null);
                }
            };
            Task damageTask = TaskExecutor.createRepeatingTask(new DamageTask(calcDamage, from, null, cancelTask, 1), 1000);
            setDamageTask(damageTask);
        }
    }

    public boolean applyStatus(MapleCharacter from, final MonsterStatusEffect status, boolean poison, long duration) {
        return applyStatus(from, status, poison, duration, false);
    }

    public boolean applyStatus(MapleCharacter from, final MonsterStatusEffect status, boolean poison, long duration, boolean venom) {
        switch (stats.getEffectiveness(status.getSkill().getElement())) {
            case IMMUNE:
            case STRONG:
            case NEUTRAL:
                return false;
            case NORMAL:
            case WEAK:
                break;
            default: {
                System.out.println("Unknown elemental effectiveness: " + stats.getEffectiveness(status.getSkill().getElement()));
                return false;
            }
        }

        if (status.getSkill().getId() == FPMage.ELEMENT_COMPOSITION) { // fp compo
            ElementalEffectiveness effectiveness = stats.getEffectiveness(Element.POISON);
            if (effectiveness == ElementalEffectiveness.IMMUNE || effectiveness == ElementalEffectiveness.STRONG) {
                return false;
            }
        } else if (status.getSkill().getId() == ILMage.ELEMENT_COMPOSITION) { // il compo
            ElementalEffectiveness effectiveness = stats.getEffectiveness(Element.ICE);
            if (effectiveness == ElementalEffectiveness.IMMUNE || effectiveness == ElementalEffectiveness.STRONG) {
                return false;
            }
        } else if (status.getSkill().getId() == NightLord.VENOMOUS_STAR || status.getSkill().getId() == Shadower.VENOMOUS_STAB || status.getSkill().getId() == NightWalker.VENOM) {// venom
            if (stats.getEffectiveness(Element.POISON) == ElementalEffectiveness.WEAK) {
                return false;
            }
        }
        if (poison && getHp() <= 1) {
            return false;
        }

        final Map<MonsterStatus, Integer> statis = status.getStati();
        if (stats.isBoss()) {
            if (!(statis.containsKey(MonsterStatus.SPEED) && statis.containsKey(MonsterStatus.NINJA_AMBUSH) && statis.containsKey(MonsterStatus.WATK))) {
                return false;
            }
        }

        for (MonsterStatus stat : statis.keySet()) {
            final MonsterStatusEffect oldEffect = stati.get(stat);
            if (oldEffect != null) {
                oldEffect.removeActiveStatus(stat);
                if (oldEffect.getStati().isEmpty()) {
                    Functions.requireNotNull(oldEffect.getCancelTask(), Task::cancel);
                    oldEffect.setCancelTask(null);
                    Functions.requireNotNull(oldEffect.getDamageTask(), Task::cancel);
                    oldEffect.setDamageTask(null);
                }
            }
        }

        final Runnable run = new Runnable() {

            @Override
            public void run() {
                if (isAlive()) {
                    byte[] packet = MaplePacketCreator.cancelMonsterStatus(getObjectId(), status.getStati());
                    map.broadcastMessage(packet, getPosition());
                    if (getController() != null && !getController().isMapObjectVisible(MapleMonster.this)) {
                        getController().getClient().announce(packet);
                    }
                }
                for (MonsterStatus stat : status.getStati().keySet()) {
                    stati.remove(stat);
                }
                setVenomMulti(0);
                Functions.requireNotNull(status.getDamageTask(), Task::cancel);
                status.setDamageTask(null);
            }
        };
        if (poison) {
            int poisonLevel = from.getSkillLevel(status.getSkill());
            int poisonDamage = Math.min(Short.MAX_VALUE, (int) (getMaxHp() / (70.0 - poisonLevel) + 0.999));
            status.setValue(MonsterStatus.POISON, poisonDamage);
            status.setDamageTask(TaskExecutor.createRepeatingTask(new DamageTask(poisonDamage, from, status, run, 0), 1000));
        } else if (venom) {
            if (from.getJob() == MapleJob.NIGHTLORD || from.getJob() == MapleJob.SHADOWER || from.getJob().isA(MapleJob.NIGHTWALKER3)) {
                int poisonLevel, matk, id = from.getJob().getId();
                int skill = (id == 412 ? NightLord.VENOMOUS_STAR : (id == 422 ? Shadower.VENOMOUS_STAB : NightWalker.VENOM));
                poisonLevel = from.getSkillLevel(SkillFactory.getSkill(skill));
                if (poisonLevel <= 0) {
                    return false;
                }
                matk = SkillFactory.getSkill(skill).getEffect(poisonLevel).getMatk();
                int luk = from.getLuk();
                int maxDmg = (int) Math.ceil(Math.min(Short.MAX_VALUE, 0.2 * luk * matk));
                int minDmg = (int) Math.ceil(Math.min(Short.MAX_VALUE, 0.1 * luk * matk));
                int gap = maxDmg - minDmg;
                if (gap == 0) {
                    gap = 1;
                }
                int poisonDamage = 0;
                for (int i = 0; i < getVenomMulti(); i++) {
                    poisonDamage += (Randomizer.nextInt(gap) + minDmg);
                }
                poisonDamage = Math.min(Short.MAX_VALUE, poisonDamage);
                status.setValue(MonsterStatus.VENOMOUS_WEAPON, poisonDamage);
                status.setDamageTask(TaskExecutor.createRepeatingTask(new DamageTask(poisonDamage, from, status, run, 0), 1000));
            } else {
                return false;
            }

        } else if (status.getSkill().getId() == Hermit.SHADOW_WEB || status.getSkill().getId() == NightWalker.SHADOW_WEB) { //Shadow Web
            status.setDamageTask(TaskExecutor.createRepeatingTask(new DamageTask(((int) (getMaxHp() / 50d + 0.999)), from, status, run, 1), 3500));
        } else if (status.getSkill().getId() == 4121004 || status.getSkill().getId() == 4221004) { // Ninja Ambush
            final Skill skill = SkillFactory.getSkill(status.getSkill().getId());
            final byte level = from.getSkillLevel(skill);
            final int damage = (int) ((from.getStr() + from.getLuk()) * (1.5 + (level * 0.05)) * skill.getEffect(level).getDamage());
            status.setValue(MonsterStatus.NINJA_AMBUSH, damage);
            status.setDamageTask(TaskExecutor.createRepeatingTask(new DamageTask(damage, from, status, run, 2), 1000));
        }
        for (MonsterStatus stat : status.getStati().keySet()) {
            stati.put(stat, status);
            alreadyBuffed.add(stat);
        }
        int animationTime = status.getSkill().getAnimationTime();
        byte[] packet = MaplePacketCreator.applyMonsterStatus(getObjectId(), status, null);
        map.broadcastMessage(packet, getPosition());
        if (getController() != null && !getController().isMapObjectVisible(this)) {
            getController().getClient().announce(packet);
        }
        status.setCancelTask(TaskExecutor.createTask(run, duration + animationTime));
        return true;
    }

    public void applyMonsterBuff(final Map<MonsterStatus, Integer> stats, final int x, int skillId, long duration, MobSkill skill, final List<Integer> reflection) {
        final Runnable run = new Runnable() {
            @Override
            public void run() {
                if (isAlive()) {
                    byte[] packet = MaplePacketCreator.cancelMonsterStatus(getObjectId(), stats);
                    map.broadcastMessage(packet, getPosition());
                    if (getController() != null && !getController().isMapObjectVisible(MapleMonster.this)) {
                        getController().getClient().announce(packet);
                    }
                    for (final MonsterStatus stat : stats.keySet()) {
                        stati.remove(stat);
                    }
                }
            }
        };
        final MonsterStatusEffect effect = new MonsterStatusEffect(stats, null, skill, true);
        byte[] packet = MaplePacketCreator.applyMonsterStatus(getObjectId(), effect, reflection);
        map.broadcastMessage(packet, getPosition());
        for (MonsterStatus stat : stats.keySet()) {
            stati.put(stat, effect);
            alreadyBuffed.add(stat);
        }
        if (getController() != null && !getController().isMapObjectVisible(this)) {
            getController().getClient().announce(packet);
        }
        effect.setCancelTask(TaskExecutor.createTask(run, duration));
    }

    public void debuffMob(int skillid) {
        //skillid is not going to be used for now until I get warrior debuff working
        MonsterStatus[] stats = {MonsterStatus.WEAPON_ATTACK_UP, MonsterStatus.WEAPON_DEFENSE_UP, MonsterStatus.MAGIC_ATTACK_UP, MonsterStatus.MAGIC_DEFENSE_UP};
        for (MonsterStatus stat : stats) {
            if (isBuffed(stat) && stati.containsKey(stat)) {
                final MonsterStatusEffect oldEffect = stati.get(stat);
                byte[] packet = MaplePacketCreator.cancelMonsterStatus(getObjectId(), oldEffect.getStati());
                map.broadcastMessage(packet, getPosition());
                if (getController() != null && !getController().isMapObjectVisible(MapleMonster.this)) {
                    getController().getClient().announce(packet);
                }
                stati.remove(stat);
            }
        }
    }

    public boolean isBuffed(MonsterStatus status) {
        return stati.containsKey(status);
    }

    public void setFake(boolean fake) {
        this.fake = fake;
    }

    public boolean isFake() {
        return fake;
    }

    public MapleMap getMap() {
        return map;
    }

    public List<Pair<Integer, Integer>> getSkills() {
        return stats.getSkills();
    }

    public boolean hasSkill(int skillId, int level) {
        return stats.hasSkill(skillId, level);
    }

    public boolean canUseSkill(MobSkill toUse) {
        if (toUse == null) {
            return false;
        }
        for (Pair<Integer, Integer> skill : usedSkills) {
            if (skill != null && skill.getLeft() == toUse.getSkillId() && skill.getRight() == toUse.getSkillLevel()) {
                return false;
            }
        }
        if (toUse.getLimit() > 0) {
            if (this.skillsUsed.containsKey(new Pair<>(toUse.getSkillId(), toUse.getSkillLevel()))) {
                int times = this.skillsUsed.get(new Pair<>(toUse.getSkillId(), toUse.getSkillLevel()));
                if (times >= toUse.getLimit()) {
                    return false;
                }
            }
        }
        if (toUse.getSkillId() == 200) {
            Collection<MapleMapObject> mmo = new ArrayList<>(getMap().getMapObjects());
            try {
                int i = 0;
                for (MapleMapObject mo : mmo) {
                    if (mo.getType() == MapleMapObjectType.MONSTER) {
                        i++;
                    }
                }
                return i <= 100;
            } finally {
                mmo.clear();
            }
        }
        return true;
    }

    public void usedSkill(final int skillId, final int level, long cooltime) {
        this.usedSkills.add(new Pair<>(skillId, level));
        if (this.skillsUsed.containsKey(new Pair<>(skillId, level))) {
            int times = this.skillsUsed.get(new Pair<>(skillId, level)) + 1;
            this.skillsUsed.remove(new Pair<>(skillId, level));
            this.skillsUsed.put(new Pair<>(skillId, level), times);
        } else {
            this.skillsUsed.put(new Pair<>(skillId, level), 1);
        }
        final MapleMonster mons = this;
        TaskExecutor.createTask(() -> mons.clearSkill(skillId, level), cooltime);
    }

    public void clearSkill(int skillId, int level) {
        int index = -1;
        for (Pair<Integer, Integer> skill : usedSkills) {
            if (skill.getLeft() == skillId && skill.getRight() == level) {
                index = usedSkills.indexOf(skill);
                break;
            }
        }
        if (index != -1) {
            usedSkills.remove(index);
        }
    }

    public int getNoSkills() {
        return this.stats.getNoSkills();
    }

    public boolean isFirstAttack() {
        return this.stats.isFirstAttack();
    }

    public int getBuffToGive() {
        return this.stats.getBuffToGive();
    }

    private final class DamageTask implements Runnable {

        private final int dealDamage;
        private MapleCharacter chr;
        private MonsterStatusEffect status;
        private Runnable cancelTask;
        private final int type;
        private MapleMap map;

        private DamageTask(int dealDamage, MapleCharacter chr, MonsterStatusEffect status, Runnable cancelTask, int type) {
            this.dealDamage = dealDamage;
            this.chr = chr;
            this.status = status;
            this.cancelTask = cancelTask;
            this.type = type;
            this.map = chr.getMap();
        }

        @Override
        public void run() {
            long localHP = getHp();
            int localDamage = dealDamage;

            if (localHP > 1) {
                if (dealDamage > localHP) {
                    localDamage = (int) (localHP - 1);
                }
                damage(chr, localDamage);
                if (type == 1) {
                    map.broadcastMessage(MaplePacketCreator.damageMonster(getObjectId(), localDamage), getPosition());
                }
            } else {
                Functions.requireNotNull(cancelTask, Runnable::run);
                // remove references
                chr = null;
                status = null;
                map = null;
            }
        }
    }

    public String getName() {
        return stats.getName();
    }

    public void addStolen(int itemId) {
        stolenItems.add(itemId);
    }

    public List<Integer> getStolen() {
        return stolenItems;
    }

    public void setTempEffectiveness(Element e, ElementalEffectiveness ee, long milli) {
        final Element fE = e;
        final ElementalEffectiveness fEE = stats.getEffectiveness(e);
        if (!stats.getEffectiveness(e).equals(ElementalEffectiveness.WEAK)) {
            stats.setEffectiveness(e, ee);
            TaskExecutor.createTask(new Runnable() {

                @Override
                public void run() {
                    stats.removeEffectiveness(fE);
                    stats.setEffectiveness(fE, fEE);
                }
            }, milli);
        }
    }

    public Collection<MonsterStatus> alreadyBuffedStats() {
        return Collections.unmodifiableCollection(alreadyBuffed);
    }

    public BanishInfo getBanish() {
        return stats.getBanishInfo();
    }

    public void setBoss(boolean boss) {
        this.stats.setBoss(boss);
    }

    public int getDropPeriodTime() {
        return stats.getDropPeriod();
    }

    public int getPADamage() {
        return stats.getPADamage();
    }

    public Map<MonsterStatus, MonsterStatusEffect> getStati() {
        return stati;
    }
}
