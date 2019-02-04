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
	Timer3 Spawner
-- Edited by --------------------------------------------------------------------------------------
	ThreeStep (based on xQuasar's King Clang spawner)

**/

importPackage(Packages.com.lucianms.client);

function init() {
    if (em.getChannel().getMap(220050200) == null) {
        print("Script AreaBossTimer3 can't initialize due to an invalid map");
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
    var lostTime2 = em.getChannel().getMap(220050200);
    var timer3 = Packages.server.life.MapleLifeFactory.getMonster(5220003);
	
	if(lostTime2.getMonsterById(5220003) != null) {
		em.schedule("start", 3 * 60 * 60 * 1000);
		return;
	}
	
    var posX;
    var posY = 1030;
    posX =  Math.floor((Math.random() * 1400) - 700);
    lostTime2.spawnMonsterOnGroundBelow(timer3, new Packages.java.awt.Point(posX, posY));
    lostTime2.broadcastMessage(Packages.tools.MaplePacketCreator.serverNotice(6, "Tick-Tock Tick-Tock! Timer makes it's presence known."));
	em.schedule("start", 3 * 60 * 60 * 1000);
}