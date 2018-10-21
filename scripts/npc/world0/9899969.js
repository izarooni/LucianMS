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
importPackage(Packages.tools);
var LifeFactory = Java.type("com.lucianms.server.life.MapleLifeFactory");
/* venem */
/* Outer Space Mini Boss and 4 planets entrance */
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
            cm.sendSimple("A #rBlack Hole#k appeared and it seems to attract everything into it. Must be some trick from #rThe Black Mage#k and his time traveling ability. Willing to take the risk? \r\n#b#L1#I am ready.#k#l\r\n\#r#L2#I am not ready yet.#k#l");
        } else if (status == 1) {
            if (selection == 1) {
                cm.warp(41, 0);
                cm.dispose();
          cm.getPlayer().getMap().broadcastMessage(MaplePacketCreator.showEffect("quest/party/clear2"));
            } else if (selection == 2) {
                cm.sendOk("Hurry up or we might miss this oppertunity!");
                cm.dispose();
            }
        }
    }
}