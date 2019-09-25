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
import com.lucianms.server.MaplePortal;
import tools.MaplePacketCreator;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @author Matze
 */
public class MapleDoor extends AbstractMapleMapObject {
    private MapleCharacter owner;
    private MapleMap townField;
    private MaplePortal townPortal;
    private MapleMap targetField;
    private Point targetPosition;

    public MapleDoor(MapleCharacter owner, Point targetPosition) {
        super();
        this.owner = owner;
        this.townPortal = getFreePortal();
        this.targetField = owner.getMap();
        this.targetPosition = targetPosition;
        this.townField = targetField.getReturnMap();
        setPosition(this.targetPosition);
    }

    public MapleDoor(MapleDoor origDoor) {
        super();
        this.owner = origDoor.owner;
        this.townField = origDoor.townField;
        this.townPortal = origDoor.townPortal;
        this.targetField = origDoor.targetField;
        this.targetPosition = origDoor.targetPosition;
        setPosition(this.townPortal.getPosition());
    }

    private MaplePortal getFreePortal() {
        List<MaplePortal> freePortals = new ArrayList<>();
        for (MaplePortal port : townField.getPortals()) {
            if (port.getType() == 6) {
                freePortals.add(port);
            }
        }
        freePortals.sort(Comparator.comparingInt(MaplePortal::getId));
        for (MapleDoor door : townField.getMapObjects(MapleDoor.class)) {
            if (door.getOwner().getParty().containsKey(door.getOwner().getId())) {
                freePortals.remove(door.getTownPortal());
            }
        }
        return freePortals.iterator().next();
    }

    @Override
    public void sendSpawnData(MapleClient client) {
        MapleCharacter player = client.getPlayer();
        boolean isOwner = owner.getId() == player.getId();
        if (player.getMapId() == targetField.getId() || (isOwner && owner.getPartyID() > 0)) {
            Point destination = townField.getId() == player.getMapId() ? townPortal.getPosition() : targetPosition;
            client.announce(MaplePacketCreator.spawnDoor(owner.getId(), destination, true));
            if (owner.getParty() != null && (isOwner || owner.getParty().containsKey(player.getId()))) {
                client.announce(MaplePacketCreator.partyPortal(townField.getId(), targetField.getId(), targetPosition));
            }
        }
        if (owner.getId() != player.getId()) {
            client.announce(MaplePacketCreator.spawnPortal(townField.getId(), targetField.getId(), targetPosition));
        }
    }

    @Override
    public void sendDestroyData(MapleClient client) {
        MapleCharacter player = client.getPlayer();
        boolean isOwner = owner == player;
        if (targetField.getId() == player.getMapId() || isOwner || owner.getParty() != null && owner.getParty().containsKey(player.getId())) {
            if (owner.getParty() != null && (isOwner || owner.getParty().containsKey(player.getId()))) {
                client.announce(MaplePacketCreator.partyPortal(999999999, 999999999, new Point(-1, -1)));
            }
            client.announce(MaplePacketCreator.removeDoor(owner.getId(), false));
            client.announce(MaplePacketCreator.removeDoor(owner.getId(), true));
        }
    }

    public void warp(MapleCharacter player, boolean toTown) {
        if (player == owner || owner.getParty() != null && owner.getParty().containsKey(player.getId())) {
            if (!toTown) {
                player.changeMap(targetField, targetPosition);
            } else {
                player.changeMap(townField, townPortal);
            }
        } else {
            player.getClient().announce(MaplePacketCreator.enableActions());
        }
    }

    public MapleCharacter getOwner() {
        return owner;
    }

    public MapleMap getTown() {
        return townField;
    }

    public MaplePortal getTownPortal() {
        return townPortal;
    }

    public MapleMap getTarget() {
        return targetField;
    }

    public Point getTargetPosition() {
        return targetPosition;
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.DOOR;
    }

}
