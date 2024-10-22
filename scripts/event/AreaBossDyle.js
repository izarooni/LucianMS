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
/**
-- Odin JavaScript --------------------------------------------------------------------------------
	Dyle Spawner
-- Edited by --------------------------------------------------------------------------------------
	ThreeStep (based on xQuasar's King Clang spawner)

**/
const MapleLifeFactory = Java.type('com.lucianms.server.life.MapleLifeFactory');
const MaplePacketCreator = Java.type('tools.MaplePacketCreator');
let nFieldID = 107000300;

function init() {
    scheduleNew();
}

function scheduleNew() {
    setupTask = em.schedule("start", 5);
}

function cancelSchedule() {
    if (setupTask != null)
        setupTask.cancel();
}

function start() {
    if (em.getChannel().isMapLoaded(nFieldID)) {
        var dangeroudCroko1 = em.getChannel().getMap(nFieldID);
        if(dangeroudCroko1.getMonsterById(6220000) != null) {
            setupTask = em.schedule("start", 3 * 60 *60 * 1000);
            return;
        }
        dangeroudCroko1.spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(6220000), new Packages.java.awt.Point(90, 119));
        dangeroudCroko1.broadcastMessage(MaplePacketCreator.serverNotice(6, "The huge crocodile Dyle has come out from the swamp."));
    }
	setupTask = em.schedule("start", 3 * 60 *60 * 1000);
}
