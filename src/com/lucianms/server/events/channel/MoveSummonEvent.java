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

import client.MapleCharacter;
import net.PacketEvent;
import server.maps.MapleSummon;
import server.movement.LifeMovementFragment;
import server.movement.MovementPacketHelper;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

import java.awt.*;
import java.util.Collection;
import java.util.List;

public final class MoveSummonEvent extends PacketEvent {

    private int objectId;

    private short xStart, yStart;

    private List<LifeMovementFragment> movements;

    @Override
    public void process(SeekableLittleEndianAccessor slea) {
        objectId = slea.readInt();
        xStart = slea.readShort();
        yStart = slea.readShort();
        movements = MovementPacketHelper.parse(null, slea);
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        Collection<MapleSummon> summons = player.getSummons().values();
        MapleSummon summon = null;
        for (MapleSummon sum : summons) {
            if (sum.getObjectId() == objectId) {
                summon = sum;
                break;
            }
        }
        if (summon != null) {
            MovementPacketHelper.updatePosition(movements, summon, 0);
            player.getMap().broadcastMessage(player, MaplePacketCreator.moveSummon(player.getId(), objectId, new Point(xStart, yStart), movements), summon.getPosition());
        }
        return null;
    }
}
