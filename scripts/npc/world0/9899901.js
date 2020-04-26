//kerrigan

const Union = Java.type("com.lucianms.client.meta.Union");
const Equip = Java.type('com.lucianms.client.inventory.Equip');
const MapleStat = Java.type('com.lucianms.client.MapleStat');
const ModifyInventory = Java.type('com.lucianms.client.inventory.ModifyInventory');
const InventoryType = Java.type('com.lucianms.client.inventory.MapleInventoryType');
const MaplePacketCreator = Java.type('tools.MaplePacketCreator');

var union = player.getUnion().getName();
var status = 0;
var currency = 4011045;
var upgrade = 0;
var aug;
var augid = 1132153;

var cost = [5, 20, 80, 150, 300];
var watt = [10, 18, 35, 45, 60];
var slots = [0, 0, 0, 0, 5];
var stat = [20, 50, 100, 200, 500];

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    }
    else {
        status++;
    }
    if (status == 1) {
        if (union.equalsIgnoreCase("none") || union == null) {
          cm.sendSimple("Hello. I am #dInvi#k, the leader of the Anguis union.\r\n#L0#Union Benefits\r\n#L1#I want to become an Anguis.");
        }
        else if (union.equalsIgnoreCase("Anguis")) {
          cm.sendSimple("Hello, disciple.\r\n#L0#I would like to upgrade my augmentation.");
        }
        else {
          cm.sendOk("Hello. I am #dInvi#k, the leader of the Anguis union.");
        }
    }
    if (status == 2) {
        if (selection == 0 && union.equalsIgnoreCase("none") || union == null) {
          cm.sendOk("Union Benefits:\r\n\r\nReceive an augmentation that can be upgraded, up to a certain limit.");
          cm.dispose();
        }
        if (selection == 0 && union.equalsIgnoreCase("Anguis")) {
          if (!cm.hasItem(augid, 1)) {
            cm.sendOk("You do not currently have an augmentation or it is equipped.");
            cm.dispose();
          }
          else if (cm.hasItem(augid, 1)) {
            let items = player.getInventory(InventoryType.EQUIP).list();
            items.forEach(item => {
                if (item.getItemId() == augid) {
                  aug = item;
                }
            });

            check = aug.getWatk();
            rank = getAugRank(check, watt);
            if (rank < 5) {
              cm.sendYesNo("Certainly. You are currently at augmentation rank " + rank + ". It will cost you " + cost[rank] + " #dAnguis coin#k to upgrade further. Would you like to upgrade? Please ensure that your augmentation is in your inventory.")
            }
            else if (rank == 5) {
              cm.sendOk("You have already reached the maximum rank for your augmentation.");
              cm.dispose();
            }
          }
        }

        if (selection == 1) {
          if (player.getUnion().getName().equals("none")) {
            union = new Union("Anguis");
            player.setUnion(union);
            cm.gainItem(augid);
            cm.sendOk("You are now an Anguis. I have given you an #daugmentation.#k Please treat it with care. You can return to me to upgrade your augmentation when you become stronger.");
            cm.dispose();
          } else {
            cm.sendOk("You appear to already be a member of another union.");
            cm.dispose();
          }
        }
    }
    if (status == 3) {
      if (cm.itemQuantity(4011045) >= cost[rank]) {
        cm.gainItem(4011045, -cost[rank])
        aug.setStr(stat[rank]);
        aug.setDex(stat[rank]);
        aug.setInt(stat[rank]);
        aug.setLuk(stat[rank]);
        aug.setWatk(watt[rank]);
        aug.setMatk(stat[rank]);

        let mods = new java.util.ArrayList();

        mods.add(new ModifyInventory(3, aug));
        mods.add(new ModifyInventory(0, aug));

        client.announce(MaplePacketCreator.modifyInventory(true, mods));

        mods.clear();

        if (aug.getPosition() < 0) {
            player.equipChanged(true);
        }
        cm.sendOk("You have successfully upgraded your augmentation. You are now at augmentation rank " + (rank + 1))
      }
      else {
        cm.sendOk("You do not have enough #dAnguis coin#k to upgrade.")
      }
    }

}

function getAugRank(check, array) {
    if (check < array[0])
      return 0;
    if (check <= array[0] && check < array[1])
      return 1;
    if (check >= array[1] && check < array[2])
      return 2;
    if (check >= array[2] && check < array[3])
      return 3;
    if (check >= array[3] && check < array[4])
      return 4;
    if (check >= array[4])
      return 5;
}
