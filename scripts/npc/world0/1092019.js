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
-- JavaScript -----------------
Lord Jonathan - Nautilus' Port
-- Created By --
Cody/Cyndicate
-- Totally Recreated by Moogra--
-- Function --
No specific function, useless text.
-- GMS LIKE --
*/
/* original by izarooni */
/* remade by kerrigan */
/* jshint esversion: 6 */

const Equip = Java.type('com.lucianms.client.inventory.Equip');
const MapleStat = Java.type('com.lucianms.client.MapleStat');
const ModifyInventory = Java.type('com.lucianms.client.inventory.ModifyInventory');
const Inventory = Java.type('com.lucianms.client.inventory.MapleInventory');
const InventoryType = Java.type('com.lucianms.client.inventory.MapleInventoryType');
const MaplePacketCreator = Java.type('tools.MaplePacketCreator');
const MapleCharacter = Java.type('com.lucianms.client.MapleCharacter');

var currency = 4000313;
var stat_count;
var item_type;
var item_array = [1132183, 1122121];

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
        cm.sendSimple("Heyo. I'm the Chirithy #dGolden Maple Leaf#k NPC. Here you can exchange golden maple leaves for an accessory of equal value.\r\n"
          + "\r\n#d#L0#I would like to make a Belt#l"
          + "\r\n#L1#I would like to make a Pendant#l"
          + "\r\n#L2#How does this work?#l");
    }
    else if (status == 1) {
      if (selection == 0) {
        item_type = item_array[selection];
        let dialog = "Certainly. How many #dgolden maple leaves#k would you like to invest into your pendant? #eNote that the maximum value for a single stat is 32767#n.";
        cm.sendGetNumber(dialog, 1, 1, 21000000);
      }
      else if (selection == 1) {
        item_type = item_array[selection];
        let dialog = "Certainly. How many #dgolden maple leaves#k would you like to invest into your belt? #eNote that the maximum value for a single stat is 32767#n.";
        //cm.sendGetNumber(dialog, 1, 1, 32767);
        cm.sendGetNumber(dialog, 1, 1, 38400);
      }
      else if (selection == 2) {
        let dialog = "Here you can exchange #dgolden maple leaves#k (GMLs) for stats on a selected type of equipment. Each GML you turn in results in +4 all stats and + 0.01 weapon attack and magic attack on your item type of choice. For example, if you turned in 1000 GML's and selected the \"belt\" option, you would receive a belt with +4000 all stats and + 10 weapon attack.";
        cm.sendOk(dialog);
        cm.dispose();
      }
    }
    else if (status == 2) {
      if (cm.itemQuantity(4000313) >= selection || player.getGMLevel()>3) {
        stat_count = selection * 4;
        let dialog = "Okay. You have elected to invest " + selection + " #dgolden maple leaves#k.\r\n\r\nThe resulting item will have the following stats:\r\n#eSTR#n: " + statCap(stat_count) 
        + "\r\n#eDEX#n: " + statCap(stat_count) 
        + "\r\n#eINT#n: " + statCap(stat_count)  
        + "\r\n#eLUK#n: " + statCap(stat_count) 
        + "\r\n#eWeapon Attack#n: " + parseInt(Math.floor(statCap(stat_count / 4) * 0.01))
        + "\r\n#eMagic Attack#n: " + parseInt(Math.floor(statCap(stat_count / 4) * 0.01))
        + "\r\n\r\nIs this correct?";
        cm.sendYesNo(dialog);
      }
      else {
        cm.sendOk("You do not have that many golden maple leaves in your inventory.");
        cm.dispose();
      }
    }
    else if (status == 3) {
    /*
      let inventory = player.getInventory(InventoryType.EQUIP);
      let item = new Equip(1322005, inventory.getNextFreeSlot(), 12);
      if (item == null) {
          cm.dispose();
          return;
      }
      
      item.setStr(statCap(stat_count));
      item.setDex(statCap(stat_count));
      item.setInt(statCap(stat_count));
      item.setLuk(statCap(stat_count));
      item.setHp(0);
      item.setMp(0);
      item.setWdef(0);
      item.setMdef(0);
      item.setAcc(0);
      item.setAvoid(0);
      item.setSpeed(0);
      item.setHands(0);
      item.setJump(0);
      item.setVicious(0);
      item.setFlag(0);
      item.setWatk(parseInt(Math.min(stat_count * 0.01)));
      item.setMatk(parseInt(Math.min(stat_count * 0.01)));

      let mods = new java.util.ArrayList();
      //mods.add(new ModifyInventory(1, item));
      mods.add(new ModifyInventory(0, item));
     // mods.add(new ModifyInventory(3, item));
      client.announce(MaplePacketCreator.modifyInventory(true, mods));
      mods.clear();
*/
    if (cm.canHold(1302000)) {
        cm.createItemWithStats(item_type, statCap(stat_count), statCap(stat_count / 4 * 0.01));   
        cm.gainItem(4000313, (-1 * stat_count / 4));
        cm.sendOk("I've just finished creating your golden maple leaf equip. Let me know what you think.");
        cm.dispose();
    }

    else {
        cm.sendOk("Your inventory is full");
        cm.dispose();
    }
  }
}

function statCap(stat) {
    return parseInt(Math.min(32767, stat));
}

