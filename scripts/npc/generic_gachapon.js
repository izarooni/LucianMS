load("scripts/util_imports.js");
load("scripts/util_transaction.js");
/* izarooni */
var status = 0;
var ticket = null;
var items = [];

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        if (items.length == 0) {
            cm.sendOk("There are currently no items avaiable to gamble for");
            cm.dispose();
        } else if (ticket == null) {
            cm.sendOk("This gachapon is not ready for public use");
            cm.dispose();
        } else {
            cm.sendNext("For only #b1 #z" + ticket + "##k, you can gamble up to " + items.length + " available items. Would you like to give it a try?");
        }
    } else if (status == 2) {
        if (cm.haveItem(ticket)) {
            var item = items[Math.floor(Math.random() * items.length)];
            if (InventoryModifier.checkSpace(client, item, 1, "")) {
                var log = player.getName() + " traded 1 " + ticket + " for 1 " + item + " from a gachapon";
                var transactionId = createTransaction(player.getId(), log);
                if (transactionId == -1) {
                    print("Error creating transaction log...");
                    print(log);
                }

                cm.gainItem(item, 1, true);
                cm.gainItem(ticket, -1, true);
                cm.sendOk("You obtained #b#v" + item + "# #z" + item + "##k, Congrats!\r\n#kHere is your transaction ID : #b" + transactionId);
            } else {
                cm.sendOk("You do not have enough #b" + ItemConstants.getInventoryType(item).name() + "#k inventory space to purchase this item");
            }
        } else {
            cm.sendOk("You need at least #b1 #z" + ticket + "##k if you want try the gachapon");
        }
        cm.dispose();
    }
}