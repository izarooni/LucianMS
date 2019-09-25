load("scripts/util_imports.js");
load("scripts/util_transaction.js");
const Crystal = Java.type("com.lucianms.constants.ServerConstants").CURRENCY;
const MesoRate = 100000; // how much will it cost for 1 Crystal
const MaximumQuantity = 100; // maximum quantity per item stack
/* izarooni */
var status = 0;

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        cm.sendNext("If you have too many Mesos to hold, you can trade them for this server's currency! #b#z" + Crystal + "#s#k cost #b" + StringUtil.formatNumber(MesoRate) + " Mesos#k each.");
    } else if (status == 2) {
        let max = Math.floor(player.getMeso() / MesoRate);
        max = Math.min(max, MaximumQuantity);
        cm.sendGetNumber("How many #b#z" + Crystal + "##K would you like?", max, 1, max);
    } else if (status == 3) {
        if (selection >= 1 && selection <= MaximumQuantity) {
            if (InventoryModifier.checkSpace(client, Crystal, selection, "")) {
                let totalMesos = (MesoRate * selection);
                let content = `${player.getName()} traded ${totalMesos} Mesos for ${selection} of item ${Crystal}`;
                let transactionId = createTransaction(cm.getDatabaseConnection(), player.getId(), content);
                player.gainMeso(totalMesos, true);
                cm.gainItem(Crystal, selection, true);
                cm.sendOk("Pleasure doing business with you!~\r\n#kHere is your transaction ID : #b" + transactionId)
            } else {
                cm.sendOk("You do not have enough #b" + ItemConstants.getInventoryType(Crystal).name() + "#k inventory space to purchase this item");
            }
        }
        cm.dispose();
    }
}
