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

// 

function start() {
    var prizes = Array(1012159, 1012165, 1012166, 1012175, 1012176, 1912179, 1012180, 1022102, 1032063, 1002977, 1052182, 1052193, 1052224, 1052225, 1052228, 1052230, 1052231, 1052232, 1052234, 1052236, 1052283, 1052174, 1051192, 1072388, 1072389, 1072437, 1102213, 1102214, 1102215, 1102217, 1102221, 1102223, 1102247, 1102233, 1102240, 1102244, 1702221, 1702223, 1702226, 1702237, 1702239, 1702253, 1702256, 1702257, 1702258, 1702259, 1702260, 1702261, 1702262, 1702263, 1702264, 1702265, 1702267, 1702268, 1702275, 1702276, 1102211, 1102187, 1102188, 1002957, 1102257, 1102252, 1702286, 1702283, 1702282, 1702281, 1702280, 1702279, 1052325, 1052326, 1052327, 1052328, 1052329, 1052330, 1052331, 1052338, 1052339, 1052340, 1052348, 1702296, 1702301, 1702302, 1102273, 1102274, 1003208, 1003216, 1072484, 1072496, 1072507, 1072509, 1052293, 1052294);
    var chances = Array(10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 5, 10, 5, 5, 5, 5, 5, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10);
    var totalodds = 0;
    var choice = 0;
    for (var i = 0; i < chances.length; i++) {
        var itemGender = (Math.floor(prizes[i]/1000)%10);
        if (cm.getPlayer().getGender() != itemGender && itemGender != 2)
            chances[i] = 0;
    }
    for (var i = 0; i < chances.length; i++)
        totalodds += chances[i];
    var randomPick = Math.floor(Math.random()*totalodds)+1;
    for (var i = 0; i < chances.length; i++) {
        randomPick -= chances[i];
        if (randomPick <= 0) {
            choice = i;
            randomPick = totalodds + 100;
        }
    }
    if (cm.isQuestStarted(2054))
        cm.gainItem(4031028,30);
    else cm.gainItem(prizes[choice],1);
    cm.sendOk("You have gained a random #bNX item#k!");
    cm.dispose();
}