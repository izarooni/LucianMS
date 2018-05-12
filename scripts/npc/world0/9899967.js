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
importPackage(Packages.tools);
var LifeFactory = Java.type("server.life.MapleLifeFactory");
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
            cm.sendSimple("Are you ready to leave?\r\n#b#L1#Yes.#k#l\r\n\#r#L2#Im not ready yet.#k#l");
        } else if (status == 1) {
            if (selection == 1) {
                cm.warp(750, 0);
                cm.dispose();
                 cm.getPlayer().getMap().broadcastMessage(MaplePacketCreator.showEffect("PSO2/stuff/4"));
                 cm.getPlayer().getMap().broadcastMessage(MaplePacketCreator.playSound("ApeEscape/YES"));
            } else if (selection == 2) {
                cm.sendOk("Talk to me anytime.");
                cm.dispose();
            }
        }
    }
}