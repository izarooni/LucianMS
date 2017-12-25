/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc> 
                       Matthias Butz <matze@odinms.de>
                       Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License version 3
    as published by the Free Software Foundation. You may not use, modify
    or distribute this program under any other version of the
    GNU Affero General Public License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

/*
 Agatha - Orbis Platform Usher
 Warp NPC
*/

var status = 0;
var maps = new Array(6, 11, 810, 271000000, 273000000, 610050000, 211060010, 219000000, 951000000, 910000025, 910001000);
var mapNames = new Array("#bTetris #k","#bWario#k", "#gCasino#k", "#dFuture Gate#k", "#rTwilight Perion#k", "#bBlackGate City#k", "#bLion King Castle", "#dCokeTown#k", "#rMonster Park#k", "#bSmash The Wall#k", "#dHarvesting Area#k");
var selectedMap = -1;

function start() {
	action(1, 0, 0);
}

function action(mode, type, selection) {
	if (status == 0) {
		var where = "Hello! #r #h #!#k I'm the #bMap#k #bWarper#k! I can warp you to:";
		for (var i = 0; i < maps.length; i++) {
			where += "\r\n#L" + i + "# " + mapNames[i] + "#l";
		}
		cm.sendSimple(where);
		status++;
	} else {
		if ((status == 1 && type == 1 && selection == -1 && mode == 0) || mode == -1) {
			cm.dispose();
		} else {
			if (status == 1) {
					cm.sendNext ("#gHa#k#rve#k #bF#k#gu#k#rn#k#b!#k ");
					selectedMap = selection;
					status++
			} else if (status == 2) {
					cm.warp(maps[selectedMap], 0);
					cm.dispose();
			}
		}
	}
}
