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
package com.lucianms.server.maps;

import com.lucianms.client.MapleCharacter;
import com.lucianms.client.MapleClient;
import com.lucianms.client.SkillFactory;
import com.lucianms.constants.ServerConstants;
import com.lucianms.constants.skills.Outlaw;
import com.lucianms.constants.skills.Ranger;
import com.lucianms.constants.skills.Sniper;
import com.lucianms.constants.skills.WindArcher;
import tools.Disposable;
import tools.MaplePacketCreator;

import java.awt.*;

/**
 * @author Jan
 */
public class MapleSummon extends AbstractAnimatedMapleMapObject implements Disposable {

    private MapleCharacter owner;
    private byte skillLevel;
    private int skill, hp;
    private SummonMovementType movementType;

    public MapleSummon(MapleCharacter owner, int skill, Point pos, SummonMovementType movementType) {
        this.owner = owner;
        this.skill = skill;
        this.skillLevel = owner.getSkillLevel(SkillFactory.getSkill(skill));
        if (skillLevel == 0) throw new RuntimeException();

        this.movementType = movementType;
        setPosition(pos);
    }

    @Override
    public void dispose() {
        owner = null;
    }

    @Override
    public void sendSpawnData(MapleClient client) {
        if (client.getPlayer().getMapId() == ServerConstants.HOME_MAP) {
            return;
        }
        client.announce(MaplePacketCreator.spawnSummon(this, false));
    }

    @Override
    public void sendDestroyData(MapleClient client) {
        client.announce(MaplePacketCreator.removeSummon(this, true));
    }

    public MapleCharacter getOwner() {
        return owner;
    }

    public int getSkill() {
        return skill;
    }

    public int getHP() {
        return hp;
    }

    public void addHP(int delta) {
        this.hp += delta;
    }

    public SummonMovementType getMovementType() {
        return movementType;
    }

    public boolean isStationary() {
        return (skill == Ranger.PUPPET || skill == Sniper.PUPPET || skill == Outlaw.OCTOPUS || skill == WindArcher.PUPPET);
    }

    public byte getSkillLevel() {
        return skillLevel;
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.SUMMON;
    }

    public final boolean isPuppet() {
        switch (skill) {
            case 3111002:
            case 3211002:
            case 13111004:
                return true;
        }
        return false;
    }
}
