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
import com.sun.java.accessibility.util.TopLevelWindowListener;
import net.AbstractMaplePacketHandler;
import server.maps.MapleDoor;
import server.maps.MapleMapObject;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

import java.util.List;

/**
 *
 * @author izarooni
 */
public class DoorHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient client) {
        MapleCharacter player = client.getPlayer();
        int doorOwnerId = slea.readInt();
        boolean toTown = (slea.readByte() == 0);
        boolean isPartyLeaderDoor = false;
        List<MapleDoor> doors = player.getDoors();

        if (doorOwnerId != player.getId()) {
            if (player.getParty() != null) {
                if (player.getParty().getLeader().getId() == doorOwnerId) {
                    doors = player.getParty().getLeader().getDoors();
                    isPartyLeaderDoor = true;
                }
            }
        }

        if (!doors.isEmpty()) {
            for (MapleDoor door : doors) {
                MapleMapObject mapObject = player.getMap().getMapObject(door.getObjectId());
                if (mapObject != null) {
                    if (door.getOwner().getId() == doorOwnerId || isPartyLeaderDoor) {
                        if (player.getMap().isTown() ^ toTown) {
                            door.warp(player, toTown);
                            return;
                        }
                    }
                }
            }
        }
        client.announce(MaplePacketCreator.enableActions());
    }
}
