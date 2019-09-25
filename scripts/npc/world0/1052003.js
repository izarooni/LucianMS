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
/* Chris
	Victoria Road : Kerning City Repair Shop (103000006)
	
	Refining NPC: 
	* Minerals
	* Jewels
	* Special - Iron Hog's Metal Hoof x 100 into Steel Plate
	* Claws
*/

var status = 0;
var selectedType = -1;
var selectedItem = -1;
var item;
var mats;
var matQty;
var cost;
var qty;
var equip;
var last_use; //last item is a use item

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == 1) {
        status ++;
    }
    else {
        cm.dispose();
    }
    if (status == 0) {
        var selStr = "Yes, I do own this forge. If you're willing to pay, I can offer you some of my services.#b"
        var options = new Array("Refine a mineral ore","Refine a jewel ore");
        for (var i = 0; i < options.length; i++){
            selStr += "\r\n#L" + i + "# " + options[i] + "#l";
        }
        cm.sendSimple(selStr);
    }
    else if (status == 1) {
        selectedType = selection;
        if (selectedType == 0){ //mineral refine
            var selStr = "So, what kind of mineral ore would you like to refine?#b";
            var minerals = new Array ("Bronze","Steel","Mithril","Adamantium","Silver","Orihalcon","Gold");
            for (var i = 0; i < minerals.length; i++){
                selStr += "\r\n#L" + i + "# " + minerals[i] + "#l";
            }
            equip = false;
            cm.sendSimple(selStr);
        }
        else if (selectedType == 1){ //jewel refine
            var selStr = "So, what kind of jewel ore would you like to refine?#b";
            var jewels = new Array ("Garnet","Amethyst","Aquamarine","Emerald","Opal","Sapphire","Topaz","Diamond","Black Crystal");
            for (var i = 0; i < jewels.length; i++){
                selStr += "\r\n#L" + i + "# " + jewels[i] + "#l";
            }
            equip = false;
            cm.sendSimple(selStr);
        }
    }
    else if (status == 2) {
        selectedItem = selection;
        if (selectedType == 0) { //mineral refine
            var itemSet = new Array(4011000,4011001,4011002,4011003,4011004,4011005,4011006);
            var matSet = new Array(4010000,4010001,4010002,4010003,4010004,4010005,4010006);
            var matQtySet = new Array(10,10,10,10,10,10,10);
            var costSet = new Array(300,300,300,500,500,500,800);
            item = itemSet[selectedItem];
            mats = matSet[selectedItem];
            matQty = matQtySet[selectedItem];
            cost = costSet[selectedItem];
        }
        else if (selectedType == 1) { //jewel refine
            var itemSet = new Array(4021000,4021001,4021002,4021003,4021004,4021005,4021006,4021007,4021008);
            var matSet = new Array(4020000,4020001,4020002,4020003,4020004,4020005,4020006,4020007,4020008);
            var matQtySet = new Array(10,10,10,10,10,10,10,10,10);
            var costSet = new Array (500,500,500,500,500,500,500,1000,3000);
            item = itemSet[selectedItem];
            mats = matSet[selectedItem];
            matQty = matQtySet[selectedItem];
            cost = costSet[selectedItem];
        }
		
        var prompt = "So, you want me to make some #t" + item + "#s? In that case, how many do you want me to make?";
        cm.sendGetNumber(prompt, 1, 1, 100);
    }
    else if (status == 3) {
        if ((cm.itemQuantity(mats) >= matQty * selection) && (cm.getMeso() >= cost * selection)) {
            cm.gainItem(mats, -matQty * selection);
            cm.gainMeso(-cost * selection);
            cm.gainItem(item, selection);
            cm.sendOk("Phew... I almost didn't think that would work for a second... Well, I hope you enjoy it, anyway.");
        }
        else {
            cm.sendOk("I cannot accept substitutes. If you don't have what I need, then I won't be able to help you.");
        } 
        cm.dispose();
    }
}