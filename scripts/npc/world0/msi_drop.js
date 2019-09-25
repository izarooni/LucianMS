/* original by izarooni */
/* remade by kerrigan */
/* jshint esversion: 6 */

const Equip = Java.type('com.lucianms.client.inventory.Equip');
const MapleStat = Java.type('com.lucianms.client.MapleStat');
const ModifyInventory = Java.type('com.lucianms.client.inventory.ModifyInventory');
const InventoryType = Java.type('com.lucianms.client.inventory.MapleInventoryType');
const MaplePacketCreator = Java.type('tools.MaplePacketCreator');
const MapleCharacter = Java.type('com.lucianms.client.MapleCharacter');

var T1 = [4005000, 4005001, 4005002, 4005003, 4005004, 4021000, 4021001, 4021002, 4021003, 4021004, 4021005, 4021006, 4021007, 4021008];
var T2 = [2340000, 2049100];
var T3 = [4001112, 4001110, 4161021, 4161018, 4161015, 4161016, 4080011];
var T4 = [2340000, 2049100];
var T1_scramble;
var T2_scramble;
var T3_scramble;
var T4_scramble;

const STAT_CHECK = "\r\n#k#eSTR#n: " + checkStat(player.getStr()) + " #k/ 32767"
  + "\r\n#k#eDEX#n: " + checkStat(player.getDex()) + " #k/ 32767"
  + "\r\n#k#eINT#n: " + checkStat(player.getInt()) + " #k/ 32767"
  + "\r\n#k#eLUK#n: " + checkStat(player.getLuk()) + " #k/ 32767\r\n";

var STAT_RESET = 4;
var STAT_MAX = 32767;
var currency = 4260002;
var TIER0 = 0;
var TIER1 = 14000;
var TIER2 = 21000;
var TIER3 = 27000;
var TIER4 = 32700;
var tier = 0;
var tier_random_scaling = [300, 400, 500, 67];
var quantity = [3, 10, 15, 25];

// Randomize drops that are used for MSI requirement
var dateScramble = new Date();
T1_scramble = T1[Math.abs(dateScramble.getHours() - (Math.floor(dateScramble.getHours() / T1.length)) * T1.length)];
T2_scramble = T2[Math.abs(dateScramble.getHours() - (Math.floor(dateScramble.getHours() / T2.length)) * T2.length)];
T3_scramble = T3[Math.abs(dateScramble.getHours() - (Math.floor(dateScramble.getHours() / T3.length)) * T3.length)];
T4_scramble = T4[Math.abs(dateScramble.getHours() - (Math.floor(dateScramble.getHours() / T4.length)) * T4.length)];

function start() {
	status = -1;
	action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    }
    else {
        status++;
    }
    if (status == 0) {
        cm.sendSimple("Hi. I'm the Chirithy #drare monster drop#k MSI NPC.\r\n"
          + "\r\n#d#L0#I would like to make a tier 1 max stat item#l"
          + "\r\n#L1#I would like to upgrade to a tier 2 max stat item#l"
          + "\r\n#L2#I would like to upgrade to a tier 3 max stat item#l"
          + "\r\n#L3#I would like to upgrade to a tier 4 max stat item#l");
    }
    else if (status == 1) {
      if (selection == 0) {
        tier = 1;
        let dialog = "Certainly. A tier 1 MSI requires the following:\r\n\r\n#v" + convertTierToDrop(tier) + "#   #z" + convertTierToDrop(tier) + "# (" + quantity[tier - 1] + ")\r\n\r\n#kWould you like to make a tier 1 MSI?";
        cm.sendYesNo(dialog);
      }
      else if (selection == 1) {
        tier = 2;
        let dialog = "Certainly. A tier 2 MSI requires the following:\r\n\r\n#v" + convertTierToDrop(tier) + "#   #z" + convertTierToDrop(tier) + "# (" + quantity[tier - 1] + ")\r\n\r\n#kWould you like to make a tier 2 MSI?";
        cm.sendYesNo(dialog);
      }
      else if (selection == 2) {
        tier = 3;
        let dialog = "Certainly. A tier 3 MSI requires the following:\r\n\r\n#v" + convertTierToDrop(tier) + "#   #z" + convertTierToDrop(tier) + "# (" + quantity[tier - 1] + ")\r\n\r\n#kWould you like to make a tier 3 MSI?";
        cm.sendYesNo(dialog);
      }
      else if (selection == 3) {
        tier = 4;
        let dialog = "Certainly. A tier 4 MSI requires the following:\r\n\r\n#v" + convertTierToDrop(tier) + "#   #z" + convertTierToDrop(tier) + "# (" + quantity[tier - 1] + ")\r\n\r\n#kWould you like to make a tier 4 MSI?";
        cm.sendYesNo(dialog);
      }
    }
    else if (status == 2) {
      let dialog = "Please select the item you would like to make into an MSI:\r\n\r\n";
      let items = player.getInventory(InventoryType.EQUIP).list();
      items.forEach(function(item) {
        dialog += `#L${item.getPosition()}##v${item.getItemId()}##l\t`;
      });
      cm.sendSimple(dialog);
    }
    else if (status == 3) {
      let inventory = player.getInventory(InventoryType.EQUIP);
      let item = inventory.getItem(selection);
      if (item == null) {
          cm.dispose();
          return;
      }
      cm.vars = { _item: item };
      let dialog = `\r\n#v${item.getItemId()}# #d#z${item.getItemId()}##k\r\n\r\nAre you certain you want to create an MSI with this item? \r\n#eNote#n: All weapon and magic attack will be removed after upgrading.`;
      cm.sendYesNo(dialog);
    }
    else if (status == 4) {
        let tier_drop = "T" + tier + "_scramble";
        let canCreate = player.getStr() == STAT_MAX &&
            player.getStr() == STAT_MAX &&
            player.getInt() == STAT_MAX &&
            player.getLuk() == STAT_MAX;
        if (canCreate && checkMSI(cm.vars._item, convertToTier(tier - 1), convertToTier(tier)) && (getDrop(convertTierToDrop(tier)) - quantity[tier - 1]) >= 0) {
            let selectedItem = cm.vars._item;
             if (tier == 4) {
              player.setMsiCreations(player.getMsiCreations() + 1);
            }

            if (!player.isDebug()) {
                player.setStr(STAT_RESET);
                player.setDex(STAT_RESET);
                player.setInt(STAT_RESET);
                player.setLuk(STAT_RESET);
                player.updateSingleStat(MapleStat.STR, STAT_RESET);
                player.updateSingleStat(MapleStat.DEX, STAT_RESET);
                player.updateSingleStat(MapleStat.INT, STAT_RESET);
                player.updateSingleStat(MapleStat.LUK, STAT_RESET);
            }

            selectedItem.setStr(convertToTier(tier) + Math.floor(Math.random() * tier_random_scaling[tier - 1]));
            selectedItem.setDex(convertToTier(tier) + Math.floor(Math.random() * tier_random_scaling[tier - 1]));
            selectedItem.setInt(convertToTier(tier) + Math.floor(Math.random() * tier_random_scaling[tier - 1]));
            selectedItem.setLuk(convertToTier(tier) + Math.floor(Math.random() * tier_random_scaling[tier - 1]));
            selectedItem.setWatk(0);
            selectedItem.setMatk(0);

            let mods = new java.util.ArrayList();
            mods.add(new ModifyInventory(3, selectedItem));
            mods.add(new ModifyInventory(0, selectedItem));
            client.announce(MaplePacketCreator.modifyInventory(true, mods));
            mods.clear();
            cm.gainItem(convertTierToDrop(tier), -1 * quantity[tier - 1]);
            if (tier == 4) {
              cm.sendOk("I've just upgraded your equip to the highest level.\r\n");
            }
            else {
              cm.sendOk("I've just finished creating your tier " + tier + " equip. If you would like to further increase the stats on this equip, please talk to me again#k. Keep in mind that requirements increase as you tier up.");
            }
        }
        else if (!canCreate && checkMSI(cm.vars._item, convertToTier(tier - 1), convertToTier(tier))) {
          cm.sendOk("Your stats are not maximized. Please speak to me again when you have 32767 in each stat.");
          cm.dispose();
        }
        else if (canCreate && !checkMSI(cm.vars._item, convertToTier(tier - 1), convertToTier(tier))) {
          cm.sendOk("The item you have selected is not a tier " + (tier - 1) + " item.");
          cm.dispose();
        }
        else if (canCreate && checkMSI(cm.vars._item, convertToTier(tier - 1), convertToTier(tier)) && (getDrop(convertTierToDrop(tier)) - quantity[tier - 1]) >= 0) {
          cm.sendOk("You do not have enough #z" + convertTierToDrop(tier) + "#k.");
          cm.dispose();
        }
        else {
            cm.sendOk("You do not meet one of the requirements to make an MSI right now.");
        }
        cm.dispose();
      }
}

function checkStat(stat) {
    switch (stat) {
      case 32767:
        return "#b" + stat;
      case !32767:
        return "#r" + stat;
    }
}

function checkRbPoints(points, tier) {
    return (points == rb_point_costs[tier - 1]) ? `#b${points}` : `#r${points}`;
}

function convertToTier(tier) {
  switch (tier) {
    case 0:
      return TIER0;
    case 1:
      return TIER1;
    case 2:
      return TIER2;
    case 3:
      return TIER3;
    case 4:
      return TIER4;
    }
}

function convertTierToDrop(tier) {
  switch (tier) {
    case 1:
      return T1_scramble;
    case 2:
      return T2_scramble;
    case 3:
      return T3_scramble;
    case 4:
      return T4_scramble;
    }
}


function checkMSI(item, tier, tier1) {
    var check = ((item.getStr() >= tier) && (item.getDex() >= tier) && (item.getInt() >= tier) && (item.getLuk() >= tier) && (item.getStr() <= tier1) && (item.getDex() <= tier1) && (item.getInt() <= tier1) && (item.getLuk() <= tier1));
    return check;
}

function getRandom(min, max) {
  return Math.floor(Math.random() * (max - min + 1) + min);
}

function getDrop(d) {
   return cm.itemQuantity(d);
}

function shuffleDrops(droplist) {
  var currentIndex = array.length, temporaryValue, randomIndex;
  while (0 !== currentIndex) {
    randomIndex = Math.floor(Math.random() * currentIndex);
    currentIndex -= 1;
    temporaryValue = array[currentIndex];
    array[currentIndex] = array[randomIndex];
    array[randomIndex] = temporaryValue;
  }
  return array;
}