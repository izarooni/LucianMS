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
import com.lucianms.nio.receive.MaplePacketReader;
import com.lucianms.server.events.PacketEvent;
import server.maps.MapleDragon;
import server.movement.LifeMovementFragment;
import server.movement.MovementPacketHelper;
import tools.MaplePacketCreator;

import java.awt.*;
import java.util.List;


public class DragonMoveEvent extends PacketEvent {

    private short xStart, yStart;

    private List<LifeMovementFragment> movements;

    @Override
    public void processInput(MaplePacketReader reader) {
        xStart = reader.readShort();
        yStart = reader.readShort();

        movements = MovementPacketHelper.parse(null, reader);
    }

    @Override
    public Object onPacket() {
        final MapleCharacter player = getClient().getPlayer();
        final MapleDragon dragon = player.getDragon();
        if (dragon != null && movements != null) {
            MovementPacketHelper.updatePosition(movements, dragon, 0);
            Point pos = new Point(xStart, yStart);
            if (player.isHidden()) {
                player.getMap().broadcastGMMessage(player, MaplePacketCreator.moveDragon(dragon, pos, movements));
            } else {
                player.getMap().broadcastMessage(player, MaplePacketCreator.moveDragon(dragon, pos, movements), dragon.getPosition());
            }
        }
        return null;
    }
}