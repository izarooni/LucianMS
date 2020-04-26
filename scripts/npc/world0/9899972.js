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
	Machine Apparatus
*/
var status = 0;

function start() {
	status = -1;
	action(1, 0, 0);
}

function action(mode, type, selection) {
	if (mode == -1) {
		cm.dispose();
	} else {
		if (mode == 0 && status == 0) {
			cm.dispose();
			return;
		}
		if (mode == 1)
			status++;
		else
			status--;
		if (status == 0) {
			cm.sendSimple("Oh no! The #rBlack Hole#k has returned! This means that we are facing chaos in the universe and that worlds are being split apart. It must be a part of #rXehanorts#k plan.\r\n\r\nHe wants to create a world split into two.\r\nOne with pure #rDarkness#k and one with pure #bLight#k. We must stop him and recreate balance! \r\n#b#L1#I will do it!#k#l\r\n\#r#L2#I do not think I am strong enough..yet.#k#l");
		} else if (status == 1) {
			if (selection == 1) {
				cm.warp(90000000, 0);
				player.announce(Packages.tools.MaplePacketCreator.showEffect("quest/party/clear3"));
				cm.dispose();
			} else if (selection == 2) {
				cm.sendOk("Okay, but hurry though.");
				cm.dispose();
			}
		}
	}
}