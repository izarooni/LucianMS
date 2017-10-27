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
package net.server.channel.handlers;

import client.MapleCharacter;
import net.PacketHandler;
import server.life.MapleMonster;
import server.life.MobSkill;
import server.life.MobSkillFactory;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.movement.LifeMovementFragment;
import server.movement.MovementPacketHelper;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.Randomizer;
import tools.data.input.SeekableLittleEndianAccessor;

import java.awt.*;
import java.util.List;

public final class MoveLifeHandler extends PacketHandler {

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
    public void process(SeekableLittleEndianAccessor slea) {
        objectId = slea.readInt();
        moveId = slea.readShort();
        bSkill = slea.readByte();
        skill = slea.readByte();
        skill_1 = slea.readByte() & 0xFF;
        skill_2 = slea.readByte();
        skill_3 = slea.readByte();
        skill_4 = slea.readByte();
        slea.skip(8);
        slea.skip(1);
        slea.skip(4);
        xStart = slea.readShort();
        yStart = slea.readShort();

        movements = MovementPacketHelper.parse(null, slea);
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
            int percHpLeft = (monster.getHp() / monster.getMaxHp()) * 100;
            if (toUse.getHP() < percHpLeft || !monster.canUseSkill(toUse)) {
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
