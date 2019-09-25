load("scripts/util_imports.js");
load("scripts/util_transaction.js");
/* izarooni*/
var SPEC_PROP = "npc.coin_shop.special_idx";
var status = 0;
var coin_id = 4011027;
var special = null;
var items = {
    regulars: [[2022282, 1, 1], [2022283, 1, 1]],
    specials: [[2340000, 1, 1], [2049100, 1, 1]],
};

if (System.getProperties().getProperty(SPEC_PROP) == null) {
    special = Math.floor(Math.random() * items.specials.length);
    System.getProperties().setProperty(SPEC_PROP, special);
} else {
    special = items.specials[parseInt(System.getProperties().getProperty(SPEC_PROP))];
}

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        cm.sendSimple("Hey #h #, would you like to exchange any #b#z" + coin_id + "##k?#b"
            + "\r\n#L0#Exchange for regular items#l"
            + "\r\n#L1#Exchange for the daily special item#l");
    } else if (status == 2) {
        if (this.choice == null) {
            this.choice = selection;
        }
        if (this.choice == 0) {
            var text = "Absolutely! What would you like to buy?\r\n";
            for (var i = 0; i < items.regulars.length; i++) {
                text += "\t#L0" + i + "##v" + items.regulars[i][0] + "##l";
            }
            cm.sendSimple(text);
        } else if (this.choice == 1) {
            if (special != null) {
                cm.sendNext("Today's special item is...\r\n#b#v" + special[0] + "# #z" + special[0] + "##k\r\nWould you like this item?");
            } else {
                cm.sendOk("Sorry, the special item is currently not ready!");
                cm.dispose();
            }
        }
    } else if (status == 3) {
        if (this.choice == 0) {
            this.item_selection = items.regulars[selection];
            cm.sendNext("Are you sure you want to trade for a #b#z" + this.item_selection[0] + "##k?");
        } else if (this.choice == 1) {
            if (cm.haveItem(coin_id, special[2])) {
                if (InventoryModifier.checkSpace(client, special[0], special[1], "")) {
                    var log = player.getName() + " traded " + special[2] + " " + coin_id + " for " + special[1] + " of item " + special[0];
                    var transactionId = createTransaction(cm.getDatabaseConnection(), player.getId(), log);
                    if (transactionId == -1) {
                        print("Error creating transaction log (coin_shop)...");
                        print(log);
                    }
                    cm.gainItem(special[0], special[1], true);
                    cm.gainItem(coin_id, -special[2], true);
                    cm.sendOk("Enjoy!~\r\n#kHere is your transaction ID : #b" + transactionId);
                } else {
                    cm.sendOk("Please make sure you have enough space in your " + ItemConstants.getInventoryType(special[0]) + " inventory");
                }
            } else {
                cm.sendOk("You do not have enough #b#z" + coin_id + "#s#k to make this purchase");
            }
            cm.dispose();
        }
    } else if (status == 4) {
        if (this.choice == 0) {
            if (cm.haveItem(coin_id, this.item_selection[2])) {
                if (InventoryModifier.checkSpace(client, this.item_selection[0], this.item_selection[1], "")) {
                    var log = player.getName() + " traded " + this.item_selection[2] + " " + coin_id + " for " + this.item_selection[1] + " of item " + this.item_selection[0];
                    var transactionId = createTransaction(cm.getDatabaseConnection(), player.getId(), log);
                    if (transactionId == -1) {
                        print("Error creating transaction log (coin_shop)...");
                        print(log);
                    }
                    cm.gainItem(this.item_selection[0], this.item_selection[1], true);
                    cm.gainItem(coin_id, -this.item_selection[2], true);
                    cm.sendOk("Enjoy!~\r\n#kHere is your transaction ID : #b" + transactionId);
                } else {
                    cm.sendOk("Please make sure you have enough space in your " + ItemConstants.getInventoryType(this.item_selection[0]) + " inventory");
                }
            } else {
                cm.sendOk("You do not have enough #b#z" + coin_id + "#s#k to make this purchase");
            }
        }
        cm.dispose();
    }
}
