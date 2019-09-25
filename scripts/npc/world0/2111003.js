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
-- Odin JavaScript --------------------------------------------------------------------------------
	Humanoid A - Magatia (GMS Like)
-- By ---------------------------------------------------------------------------------------------
	Maple4U
-- Version Info -----------------------------------------------------------------------------------
    1.1 - Shortened 3x by Moogra
	1.0 - First Version by Maple4U
---------------------------------------------------------------------------------------------------
*/

// Modified by Kerrigan, Chirithy coin NPC

var status = 0;

function action (mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    }
    else {
        status++;
    }
    if (status == 1) {
        cm.sendSimple("Beep-boop. Just kidding, I'm a real person. Anyway, you can convert your mesos to #bChirithy coin#k here.\r\n\r\n#d#L0#I want to convert 2 billion meso to 1 #bChirithy coin#k");
    }
    else if (status == 2) { 
        if (selection == 0) {
            if (cm.getMeso() >= 2000000000) {
                gainPoints(4260002, 1)
                player.gainMeso(-2000000000, true);
                cm.sendOk("Done. Enjoy your shiny new coin.");
            }
            else {
                cm.sendOk("You don't appear to have 2 billion mesos.");
                cm.dispose();
            }
        }

    }
}

function gainPoints(s, amt) {
    if (typeof s == 'number') {
        cm.gainItem(s, amt, true);
        return true;
    }
    switch (s) {
        default: return false;
        case "PQ points":
            player.addPoints("pq", amt);
            return true;
        case "event points":
            player.addPoints("ep", amt);
            return true;
        case "donor points":
            player.addPoints("dp", amt);
            return true;
        case "fishing points":
            player.addPoints("fp", amt);
            return true;
        case "vote points":
            player.addPoints("vp", amt);
            return true;
    }
}
