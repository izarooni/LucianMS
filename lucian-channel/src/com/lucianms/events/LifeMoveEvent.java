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

import com.lucianms.client.MapleCharacter;
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.life.MapleMonster;
import com.lucianms.server.life.MobSkill;
import com.lucianms.server.life.MobSkillFactory;
import com.lucianms.server.maps.MapleMapObject;
import com.lucianms.server.maps.MapleMapObjectType;
import com.lucianms.server.movement.LifeMovementFragment;
import com.lucianms.server.movement.MovementPacketHelper;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.Randomizer;

import java.awt.*;
import java.util.List;

public final class LifeMoveEvent extends PacketEvent {

    private int objectId;
    private int skill_1;
    private int skill_2;
    private int skill_3;
    private int skill_4;

    private short moveId;
    private short xStart, yStart;

    private byte bSkill;
    private byte skill;

    private List<LifeMovementFragment> movements;

    @Override
    public void processInput(MaplePacketReader reader) {
        objectId = reader.readInt();
        moveId = reader.readShort();
        bSkill = reader.readByte();
        skill = reader.readByte();
        skill_1 = reader.readByte() & 0xFF;
        skill_2 = reader.readByte();
        skill_3 = reader.readByte();
        skill_4 = reader.readByte();
        reader.readByte();
        reader.readInt();
        reader.readInt();
        reader.readInt();
        xStart = reader.readShort();
        yStart = reader.readShort();

        movements = MovementPacketHelper.parse(null, reader);
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        MapleMapObject mmo = player.getMap().getMapObject(objectId);
        if (mmo == null || mmo.getType() != MapleMapObjectType.MONSTER) {
            return null;
        }
        MapleMonster monster = (MapleMonster) mmo;
        MobSkill toUse = null;
        if (bSkill == 1 && monster.getNoSkills() > 0) {
            int random = Randomizer.nextInt(monster.getNoSkills());
            Pair<Integer, Integer> skillToUse = monster.getSkills().get(random);
            toUse = MobSkillFactory.getMobSkill(skillToUse.getLeft(), skillToUse.getRight());
            int percHpLeft = (int) ((monster.getHp() / monster.getMaxHp()) * 100);
            if (toUse != null && (toUse.getHP() < percHpLeft || !monster.canUseSkill(toUse))) {
                toUse = null;
            }
        }
        if ((skill_1 >= 100 && skill_1 <= 200) && monster.hasSkill(skill_1, skill_2)) {
            MobSkill skillData = MobSkillFactory.getMobSkill(skill_1, skill_2);
            if (skillData != null && monster.canUseSkill(skillData)) {
                skillData.applyEffect(player, monster, true);
            }
        }
        Point startPos = new Point(xStart, yStart);
        if (monster.getController() != player) {
            if (monster.isAttackedBy(player)) {// aggro and controller change
                monster.switchController(player, true);
            } else {
                return null;
            }
        } else if (skill == -1 && monster.isControllerKnowsAboutAggro() && !monster.isMobile() && !monster.isFirstAttack()) {
            monster.setControllerHasAggro(false);
            monster.setControllerKnowsAboutAggro(false);
        }
        boolean aggro = monster.isControllerHasAggro();
        if (toUse != null) {
            getClient().announce(MaplePacketCreator.moveMonsterResponse(objectId, moveId, monster.getMp(), aggro, toUse.getSkillId(), toUse.getSkillLevel()));
        } else {
            getClient().announce(MaplePacketCreator.moveMonsterResponse(objectId, moveId, monster.getMp(), aggro));
        }
        if (aggro) {
            monster.setControllerKnowsAboutAggro(true);
        }
        if (movements != null) {
            player.getMap().broadcastMessage(player, MaplePacketCreator.moveMonster(bSkill, skill, skill_1, skill_2, skill_3, skill_4, objectId, startPos, movements), monster.getPosition());
            MovementPacketHelper.updatePosition(movements, monster, -1);
            player.getMap().moveMonster(monster, monster.getPosition());
        }
        return null;
    }
}
