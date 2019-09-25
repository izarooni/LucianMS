/* izarooni */
/* kerrigan */
/* jshint esversion: 6 */

const Equip = Java.type('com.lucianms.client.inventory.Equip');
const MapleStat = Java.type('com.lucianms.client.MapleStat');
const ModifyInventory = Java.type('com.lucianms.client.inventory.ModifyInventory');
const InventoryType = Java.type('com.lucianms.client.inventory.MapleInventoryType');
const MaplePacketCreator = Java.type('tools.MaplePacketCreator');

let status = 0;
var sacrifice;
var target;
let base = 0;
var stat_list = ["str", "dex", "int", "luk", "hp", "mp", "watk", "matk", "wdef", "mdef", "acc", "avoid", "speed", "jump"];
var coin_req = 75;
var rbpt_req = 500;
var cs_req = 8;
var banned = [1132183, 1122121, 1702227]

function action(mode, type, selection) {
  if (mode < 1) {
      cm.dispose();
      return;
  } else {
      status++;
  }
  if (status == 1) {
    cm.sendSimple("Heya. I perform polymerizations. What would you like to do?#d\r\n\r\n#L0#What is a polymerization?#l\r\n#L1#What are the requirements for polymerization?#l\r\n#L2#Let me perform a polymerization#l");
  }
  else if (status == 2) {
    if (selection == 0) {
      cm.sendNext("A polymerization is an advanced item fusion. Two items are selected: a sacrifice and a recipient. If a stat value on the sacrifice exceeds the one on the recipient, then that stat will be transferred onto the recipient item. #eNote that all stats are erased from the sacrificed item#n, regardless of if they replace the corresponding stat on the recipient.");
      status = 1;
    }
    if (selection == 1) {
      cm.sendNext("Sure thing. Here are the requirements for a transmutation:\r\n\r\n#d500 Rebirth points\r\n" + coin_req + " #bChirithy coin#d\r\n" + cs_req + " #rChaos scrolls#n");
    }
    else if (selection == 2) {
      let content = "What item would you like to take the stats from?\r\n";
      let items = player.getInventory(InventoryType.EQUIP).list();
      content += "Equips currently in your inventory:\r\n\r\n";
      items.forEach(item => {
          content += `#L${item.getPosition()}##v${item.getItemId()}##l\t`;
      });
      cm.sendSimple(content);
      }
  }
  else if (status == 3) {
    let inventory = player.getInventory(InventoryType.EQUIP);
    let item = inventory.getItem(selection);
    if (item == null) {
        cm.dispose();
        return;
    }
    sacrifice = item;
    let content = `You have selected:\r\n\r\n#v${item.getItemId()}# #d#z${item.getItemId()}##k\r\n\r\nAre you sure you want to siphon the stats from this item?\r\n#eNote: all existing stats will be wiped from the current item#n`;
    cm.sendYesNo(content);
  }
  else if (status == 4) {
      let content = "What item would you like to transmute these stats into?\r\nEquips in your inventory:\r\n\r\n";
      let items = player.getInventory(InventoryType.EQUIP).list();
      items.forEach(item => {
          content += `#L${item.getPosition()}##v${item.getItemId()}##l\t`;
      });
      cm.sendSimple(content);
  }
  else if (status == 5) {
    let inventory = player.getInventory(InventoryType.EQUIP);
    let item = inventory.getItem(selection);
    if (item == null) {
        cm.dispose();
        return;
    }
    target = item;
    let content = `You have selected:\r\n\r\n#v${item.getItemId()}# #d#z${item.getItemId()}##k\r\n\r\nAre you sure you want this item to receive the sacrificed item's stats?`;
    cm.sendYesNo(content);
  }
  else if (status == 6) {
    if (sacrifice.getPosition() == target.getPosition()) {
      cm.sendOk("You cannot transmute an item's stats onto itself.");
      cm.dispose();
      return;
    }
    else if (contains(banned, sacrifice.getItemId()) || contains(banned, target.getItemId())) {
      cm.sendOk("This item is banned from being polymerized.");
      cm.dispose();
      return;
    }
    else {
      cm.sendYesNo("Alright, I've got it. I'm going perform a polymerization using the following sacrificed stats (only the higher stat value will be fused onto the final item!): "
      + "\r\n\r\n#d#eSacrifice#n: #v" + sacrifice.getItemId() + "#" + "\r\n\r\n"
      + "#eSTR#n: " + sacrifice.getStr() + "\r\n"
      + "#eDEX#n: " + sacrifice.getDex() + "\r\n"
      + "#eINT#n: " + sacrifice.getInt() + "\r\n"
      + "#eLUK#n: " + sacrifice.getLuk() + "\r\n"
      + "#eWeapon Attack#n: " + sacrifice.getWatk() + "\r\n"
      + "#eMagic Attack#n: " + sacrifice.getMatk() + "\r\n"
      + "#eWeapon Defense#n: " + sacrifice.getWdef() + "\r\n"
      + "#eMagic Defense#n: " + sacrifice.getMdef() + "\r\n\r\n"
      + "#eRecipient#n: " + "#v" + target.getItemId() + "#");
    }
  }
  else if (status == 7) {
    if ((checkRequirement(4260002, coin_req) && checkRequirement(2049100, cs_req) && checkRequirement("rbpoints", rbpt_req)) || player.getName() === 'iDeeKay' ){
      memory = sacrifice.duplicate();

      for (var i = 0; i < stat_list.length; i++) {
        sacrifice.setStat(stat_list[i], 0);
      }

      if (memory.getStr() > target.getStr()) {
        target.setStr(memory.getStr());
      }
      if (memory.getDex() > target.getDex()) {
        target.setDex(memory.getDex());
      }
      if (memory.getInt() > target.getInt()) {
        target.setInt(memory.getInt());
      }
      if (memory.getLuk() > target.getLuk()) {
        target.setLuk(memory.getLuk());
      }
      if (memory.getWdef() > target.getWdef()) {
        target.setWdef(memory.getWdef());
      }
      if (memory.getHp() > target.getHp()) {
        target.setHp(memory.getHp());
      }
      if (memory.getMp() > target.getMp()) {
        target.setMp(memory.getMp());
      }
      if (memory.getMdef() > target.getMdef()) {
        target.setMdef(memory.getMdef());
      }
      if (memory.getWatk() > target.getWatk()) {
        target.setWatk(memory.getWatk());
      }
      if (memory.getMatk() > target.getMatk()) {
        target.setMatk(memory.getMatk());
      }
      if (memory.getAvoid() > target.getAvoid()) {
        target.setAvoid(memory.getAvoid());
      }
      if (memory.getAcc() > target.getAcc()) {
        target.setAcc(memory.getAcc());
      }
      if (memory.getSpeed() > target.getSpeed()) {
        target.setSpeed(memory.getSpeed());
      }
      if (memory.getJump() > target.getJump()) {
        target.setJump(memory.getJump());
      }

      let sacrificemods = new java.util.ArrayList();
      let targetmods = new java.util.ArrayList();

      sacrificemods.add(new ModifyInventory(3, sacrifice));
      sacrificemods.add(new ModifyInventory(0, sacrifice));
      targetmods.add(new ModifyInventory(3, target));
      targetmods.add(new ModifyInventory(0, target));

      client.announce(MaplePacketCreator.modifyInventory(true, sacrificemods));
      client.announce(MaplePacketCreator.modifyInventory(true, targetmods));

      sacrificemods.clear();
      targetmods.clear();

      if (sacrifice.getPosition() < 0) {
          player.equipChanged(true);
      }

      if (target.getPosition() < 0) {
          player.equipChanged(true);
      }

      cm.gainItem(4260002, -1 * coin_req);
      cm.gainItem(2049100, -1 * cs_req);
      player.setRebirthPoints(player.getRebirthPoints() - rbpt_req);

      cm.sendOk("You have successfully performed a polymerization. Congratulations..!");
      cm.dispose();
    }
    else {
      cm.sendOk("You do not meet one or more of the requirements to perform a polymerization.");
      cm.dispose();
    }
  }
}

function checkRequirement(currency, required) {
  if (typeof currency == 'number') {
      if (cm.itemQuantity(currency) >= required) {
        return true;
      }
  }
  if (currency == "rbpoints") {
      if (player.getRebirthPoints() >= required) {
        return true;
      }
  }
}

function contains(banlist, id) {
  var i = banlist.length;
  while (i --) {
     if (banlist[i] === id) {
         return true;
     }
  }
  return false;
}
