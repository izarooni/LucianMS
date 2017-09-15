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
package client.status;

import client.Skill;
import scheduler.Task;
import server.life.MobSkill;
import tools.ArrayMap;

import java.util.Map;

public class MonsterStatusEffect {

    private Map<MonsterStatus, Integer> stati;
    private Skill skill;
    private MobSkill mobskill;
    private boolean monsterSkill;
    private Task cancelTask = null;
    private Task damageTask = null;

    public MonsterStatusEffect(Map<MonsterStatus, Integer> stati, Skill skillId, MobSkill mobskill, boolean monsterSkill) {
        this.stati = new ArrayMap<>(stati);
        this.skill = skillId;
        this.monsterSkill = monsterSkill;
        this.mobskill = mobskill;
    }

    public Map<MonsterStatus, Integer> getStati() {
        return stati;
    }

    public void setValue(MonsterStatus status, Integer newVal) {
        stati.put(status, newVal);
    }

    public Skill getSkill() {
        return skill;
    }

    public boolean isMonsterSkill() {
        return monsterSkill;
    }

    public Task getCancelTask() {
        return cancelTask;
    }

    public void setCancelTask(Task cancelTask) {
        this.cancelTask = cancelTask;
    }

    public void removeActiveStatus(MonsterStatus stat) {
        stati.remove(stat);
    }

    public Task getDamageTask() {
        return damageTask;
    }

    public void setDamageTask(Task damageTask) {
        this.damageTask = damageTask;
    }

    public MobSkill getMobSkill() {
        return mobskill;
    }
}
