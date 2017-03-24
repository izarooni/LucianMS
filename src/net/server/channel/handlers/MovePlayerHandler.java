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
import java.util.List;

import client.MapleStat;
import net.server.channel.handlers.AbstractMovementPacketHandler;
import server.movement.LifeMovementFragment;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public final class MovePlayerHandler extends AbstractMovementPacketHandler {
    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient client) {
        MapleCharacter player = client.getPlayer();
        slea.skip(9);
        final List<LifeMovementFragment> res = parseMovement(slea);
        if (res != null) {
            updatePosition(res, player, 0);
            player.getMap().movePlayer(player, player.getPosition());
            if (player.isHidden()) {
                player.getMap().broadcastGMMessage(player, MaplePacketCreator.movePlayer(player.getId(), res), false);
            } else {
                player.getMap().broadcastMessage(player, MaplePacketCreator.movePlayer(player.getId(), res), false);
            }
        }
        if (player.getMap().getAutoKillPosition() != null) {
            if (player.getPosition().getY() >= player.getMap().getAutoKillPosition().getY()) {
                player.setHp(0);
                player.updateSingleStat(MapleStat.HP, 0);
            }
        }
    }
}
