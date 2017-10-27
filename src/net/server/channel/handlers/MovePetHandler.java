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
import client.MapleClient;
import net.PacketHandler;
import server.movement.LifeMovement;
import server.movement.LifeMovementFragment;
import server.movement.MovementPacketHelper;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

import java.util.List;

public final class MovePetHandler extends PacketHandler {

    private int petId;

    private List<LifeMovementFragment> movements;

    @Override
    public void process(SeekableLittleEndianAccessor slea) {
        petId = slea.readInt();
        slea.skip(8);
        movements = MovementPacketHelper.parse(null, slea);
    }

    @Override
    public Object onPacket() {
        MapleCharacter player = getClient().getPlayer();
        byte index = player.getPetIndex(petId);
        if (index == -1) {
            return null;
        }
        player.getPet(index).updatePosition(movements);
        player.getMap().broadcastMessage(player, MaplePacketCreator.movePet(player.getId(), petId, index, movements), false);
        return null;
    }
}
