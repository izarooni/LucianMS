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

/**
 Joel (Ellinia Ticket Usher)
**/

var status = 0;

function start() {
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (status == 0) {
        cm.sendYesNo("Do you want to go to No Patch FM?\r\n#r(If u dont have any Map.wz update, this is the place to talk to the NPCs.)#k");
        status++;
    } else {
        if ((status == 1 && type == 1 && selection == -1 && mode == 0) || mode == -1) {
            cm.dispose();
        } else {
            if (status == 1) {
                    cm.sendNext ("Alright, see you next time.");
                    status++
            } else if (status == 2) {
                    cm.warp(990000600, 0);
                    cm.dispose();
            }
        }
    }
}
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

/**
 Joel (Ellinia Ticket Usher)
**/

importPackage(Packages.tools);
var LifeFactory = Java.type("server.life.MapleLifeFactory");
var status = 0;

function start() {
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (status == 0) {
        cm.sendYesNo("Welcome to #rLucianMS#k\r\nTo play this server you need our custom WZ files but since you are talking to me I assume you are already set and ready to go.\r\nIf you find a bug, crash or find any other issues please sumbit it on our forums or to an GM/Admin on our discord!\r\n\r\n#dAre you ready to continue in the tutorial?#k\r\nThen click the #gYes#k button.");
        status++;
    } else {
        if ((status == 1 && type == 1 && selection == -1 && mode == 0) || mode == -1) {
            cm.dispose();
        } else {
            if (status == 1) {
                    cm.sendNext ("Alright, talk to me when you are ready.");
                    status++
            } else if (status == 2) {
                    cm.warp(90000001, 0);
                    cm.dispose();
                    cm.getPlayer().getMap().broadcastMessage(MaplePacketCreator.showEffect("PSO2/stuff/1"));
            }
        }
    }
}
