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
	Seruf Spawner
-- Edited by --------------------------------------------------------------------------------------
	ThreeStep (based on xQuasar's King Clang spawner)

**/
const nFieldID = 230020100;

function init() {
    if (em.getChannel().getMap(230020100) == null) {
        print("Script AreaBossSeruf can't initialize due to an invalid map");
    } else {
        scheduleNew();
    }
}

function scheduleNew() {
    setupTask = em.schedule("start", 60 * 1000 * 5); // first one spawns in 5 minutes after restart, all others respawn 45 minutes after death (set in maplemap.java)
}

function cancelSchedule() {
    if (setupTask != null)
        setupTask.cancel();
}

function start() {
    if (em.getChannel().isMapLoaded(nFieldID)) {
        var towerMap = em.getChannel().getMap(nFieldID);
        var seruf = Packages.server.life.MapleLifeFactory.getMonster(4220001);

        if(towerMap.getMonsterById(4220001) != null) {
            em.schedule("start", 3 * 60 * 60 * 1000);
            return;
        }

        var posX;
        var posY = 520;
        posX =  Math.floor((Math.random() * 2300) - 1500);
        towerMap.spawnMonsterOnGroundBelow(seruf, new Packages.java.awt.Point(posX, posY));
        towerMap.broadcastMessage(Packages.tools.MaplePacketCreator.serverNotice(6, "A strange shell has appeared from a grove of seaweed"));
    }
	em.schedule("start", 3 * 60 * 60 * 1000);
}
