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

/* Guy in dollhouse map
*/
/*
var greeting;

function start() {
    var greeting = "Thank you for finding the pendulum. Are you ready to return to Eos Tower?";
    if (cm.isQuestStarted(3230)) {
        if (cm.haveItem(4031094)) {
            cm.completeQuest(3230);
            cm.gainItem(4031094, -1);
        } else
            greeting = "You haven't found the pendulum yet. Do you want to go back to Eos Tower?";
    }
    cm.sendYesNo(greeting);
}

function action(mode, type, selection) {
    if (mode > 0)
        cm.warp(221024400,0);
    cm.dispose();
}*/
load("nashorn:mozilla_compat.js"); // to load all boss pq classes
importPackage(Packages.com.lucianms.features.bpq);

let arena = { pq: BHellMode, mode: "hell difficulty" };

const b = 3994115; // difficulty represented as an item
/* izarooni */
let status = 0;

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        let text = "Would you like to enter the #dBoss Arena#k?";
      //  cm.sendYesNo(text);
    }
    else if (status == 2) {
        let pq = arena.pq;
        arena.pq = new pq(client.getChannel());
        cm.sendSimple("I want to enter...#d"
            + "\r\n#L0#Solo#l"
            + "\r\n#L1#In a party#l");
    }
    else if (status == 3) {
        if (selection == 0) {
            arena.pq.registerPlayer(player);
            arena.pq.begin();
        }
        else if (selection == 1) {
            if (cm.getParty() != null) {
                if (cm.isLeader()) {
                    let iter = cm.getPartyMembers().iterator();
                    let map = player.getMap();
                    while (iter.hasNext()) {
                        let n = iter.next();
                        if (map.getCharacterById(n.getId()) != null) {
                            arena.pq.registerPlayer(n);
                        }
                    }
                    arena.pq.begin();
                } else {
                    cm.sendOk("Only the party leader may decide when to enter the #dboss arena#k.");
                }
            } else {
                cm.sendOk("You are currently not in a party.");
            }
        }
        cm.dispose();
    }
}
