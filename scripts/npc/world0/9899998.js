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
//importPackage(Packages.tools);
var LifeFactory = Java.type("com.lucianms.server.life.MapleLifeFactory");
const Equip = Java.type('com.lucianms.client.inventory.Equip');
const MapleStat = Java.type('com.lucianms.client.MapleStat');
const ModifyInventory = Java.type('com.lucianms.client.inventory.ModifyInventory');
const InventoryType = Java.type('com.lucianms.client.inventory.MapleInventoryType');
const MaplePacketCreator = Java.type('tools.MaplePacketCreator');
const MapleCharacter = Java.type('com.lucianms.client.MapleCharacter');
//Jimmy!

//TODO heavily refactor. This is drunk coding ftw
var status = 0;
var MINOR = 1;
var NORMAL = 2;
var MAJOR = 3;
var upgradeType = 0;
var upgradeItem = 0;
var tier1 = 4000000;
var tier2 = 4000000;
var tier3 = 4000000;

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 0) {
        //cm.sendSimple("#L1#k den.#k#l\r\n\#r#L2#wat.#k#l");
        cm.sendSimple("Hey I'm the Chirithy #dCrafting/Upgrade#k NPC.\r\n"
            + "\r\n#d#L0#I would like to do a minor upgrade#l"
            + "\r\n#L1#I would like to do a normal upgrade#l"
            + "\r\n#L2#I would like to do a major upgrade#l"
            + "\r\n#L3#What do you do?#l"
            + "\r\n#L4#Make me a special ring#l");
    } else if (status == 1) {
        if (selection == 0) {
            upgradeType = MINOR;
            upgradeItem = tier1;
            let dialog = "Certainly. We need 1 snail shell #i" + tier1 + "# and we can upgrade your stats from +1~3!";
            cm.sendYesNo(dialog);
        } else if (selection == 1) {
            upgradeType = NORMAL;
            upgradeItem = tier2;
            let dialog = "Certainly. We need 1 snail shell #i" + tier2 + "# and we can upgrade your stats from +3~9!";
            cm.sendYesNo(dialog);
        } else if (selection == 2) {
            upgradeType = MAJOR;
            upgradeItem = tier3;
            let dialog = "Certainly. We need 1 snail shell #i" + tier3 + "# and we can upgrade your stats from +5~15!";
            cm.sendYesNo(dialog);
        } else if (selection == 3) {

            let dialog = "In short, I take specific items from you that are gained through daily dungeons, and give you an upgrade on your item based on the tier. The items required and upgrade amounts will be told in each option you choose.";
            cm.sendOk(dialog);
            cm.dispose();
        } else if (selection == 4) {

            let dialog = "BETA ONLY. FREE RING FOR YOU!!";
            cm.createItemWithStats(1112400,1,1);
            cm.sendOk(dialog);
            cm.dispose();
        }

    } else if (status == 2) {
        let dialog = "Please select the item you would like to make into an MSI:\r\n\r\n";
        let items = player.getInventory(InventoryType.EQUIP).list();
        items.forEach(function (item) {
            dialog += `#L${item.getPosition()}##v${item.getItemId()}##l\t`;
        });
        cm.sendSimple(dialog);
    } else if (status == 3) {
        let inventory = player.getInventory(InventoryType.EQUIP);
        let item = inventory.getItem(selection);
        if (item == null) {
            cm.dispose();
            return;
        }
        cm.vars = {_item: item};
        let dialog = `\r\n#v${item.getItemId()}# #d#z${item.getItemId()}##k\rnAre you certain you want to upgrade this item?`;
        cm.sendYesNo(dialog);
    } else if (status == 4) {

        let selectedItem = cm.vars._item;
        let canCreate = cm.haveItem(upgradeItem, 1) && (selectedItem.getUpgradeSlots() > 0); // items required
        if (canCreate) {
            cm.gainItem(upgradeItem, -1);

            doUpgrade(selectedItem, upgradeType);
            dialog = "Upgraded!";
        } else {
            dialog = "You don't have enough" +upgradeItem+ " or the item you chose doesn't have enough upgrade slots";
        }
        cm.sendOk(dialog);
        cm.dispose();
    }

}

function doUpgrade(selectedItem, tier) {

    var multiplier = 1;
    switch (tier) {
        case MINOR:
            multiplier = 1;
            break;
        case NORMAL:
            multiplier = 3;
            break;
        case MAJOR:
            multiplier = 5;
            break;
        default:
            break;
    }
    var min = 1 * multiplier;
    var max = 3 * multiplier;

    if (selectedItem.getStr() > 0) {
        selectedItem.setStr(selectedItem.getStr() + randNum(min, max));
    }
    if (selectedItem.getDex() > 0) {
        selectedItem.setDex(selectedItem.getDex() + randNum(min, max));
    }
    if (selectedItem.getInt() > 0) {
        selectedItem.setInt(selectedItem.getInt() + randNum(min, max));
    }
    if (selectedItem.getLuk() > 0) {
        selectedItem.setLuk(selectedItem.getLuk() + randNum(min, max));
    }
    if (selectedItem.getWatk() > 0) {
        selectedItem.setWatk(selectedItem.getWatk() + randNum(min, max));
    }
    if (selectedItem.getMatk() > 0) {
        selectedItem.setMatk(selectedItem.getMatk() + randNum(min, max));
    }
    if (selectedItem.getWdef() > 0) {
        selectedItem.setWdef(selectedItem.getWdef() + randNum(min, max));
    }
    if (selectedItem.getMdef() > 0) {
        selectedItem.setMdef(selectedItem.getMdef() + randNum(min, max));
    }
    if (selectedItem.getJump() > 0) {
        selectedItem.setJump(selectedItem.getJump() + randNum(min, max));
    }

    selectedItem.setUpgradeSlots(selectedItem.getUpgradeSlots - 1);
    let mods = new java.util.ArrayList();
    mods.add(new ModifyInventory(3, selectedItem));
    mods.add(new ModifyInventory(0, selectedItem));
    client.announce(MaplePacketCreator.modifyInventory(true, mods));
    mods.clear();
}

function randNum(min, max) { // min and max included
    return Math.floor(Math.random() * (max - min + 1) + min);
}