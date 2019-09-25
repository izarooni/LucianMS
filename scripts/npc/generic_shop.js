load("scripts/util_imports.js");
load("scripts/util_transaction.js");
/* izarooni */
let status = 0;
let broken = null;
let display = null;
const dialog = {
    introSpoke: false, // backwards compatibility
    intro: null,
    first: "You currently have #b{PointsQuantity} {PointsName}#k\r\nIs there anything you would like to buy?\r\n#b",
    second: "Absolutely, what item interests you?\r\n#b"
};
const items = {
    get: function(idx, ob) {
        var i = 0;
        for (var n in items) {
            if (!(items[n] instanceof Function) && (i++) == idx) {
                return ob ? items[n] : n;
            }
        }
        return null;
    }
};

function action(mode, type, selection) {
    if (display == null) {
        display = (typeof pointsType == 'number') ? "#z" + pointsType + "#" : pointsType;
    }
    if (dialog.intro != null && !dialog.introSpoke) {
        status = -1;
    }
    if (mode < 1) {
        if (status == 3) {
            status = 2;
        } else {
            cm.dispose();
            return;
        }
    } else {
        status++;
    }
    if (status == 0) {
        cm.sendNext(dialog.intro);
        dialog.introSpoke = true;
    } else if (status == 1) {
        let isShopEmpty = Object.keys(items).length == 1;
        if (isShopEmpty) {
            cm.sendOk("Someone stole my shop items, I have nothing to sell!\r\nCome again another time when I have re-stocked my inventory.");
            return cm.dispose();
        } else if (broken != null) {
            if (player.isGM()) {
                cm.sendOk("Hey #h #, I see you're a GM. An error occurred while parsing the " + display + " trader data. Please let a developer know ASAP!\r\n#r" + broken);
            } else {
                cm.sendOk("Oh no, my shop is a mess! Please report to an Administrator to help me get a shop setup again");
            }
            return cm.dispose();    
        }
        let text = dialog.first;
        text = text.replace(/{PointsQuantity}/g, getPoints(pointsType));
        text = text.replace(/{PointsName}/g, display);
        var i = 0;
        for (var it in items) {
            if (!(items[it] instanceof Function)) {
                text += "\r\n#L" + (i++) + "#" + it + "#l";
            }
        }
        cm.sendSimple(text);
    } else if (status == 2) {
        if (this.sub == null) {
            this.sub = items.get(selection, true);
        }
        if (this.sub != null && this.sub.length > 0) {
            let subName = items.get(selection, false);
            let text = dialog.second;
            for (let i = 0; i < this.sub.length; i++) {
                let itemID = this.sub[i][0];
                if (subName == "Chairs") {
                    // a lot of custom chairs don't have names, thus viewing the item in shop via name is impossible.
                    // instead, showing the image of the item (although small) is much better than showing nothing
                    text += `#L${i}# #v${itemID}#`;
                    if (i % 5 == 0 && i > 0) text += "\r\n";
                } else {
                    text += `\r\n#L${i}# #v${itemID}# #z${itemID}##l`;
                }
            }
            cm.sendSimple(text);
        } else if (this.sub.length == 0) {
            cm.sendNext("Sorry this part of the shop is empty!");
            this.sub = null;
            status = 0;
        } else {
            cm.sendOk("Error(" + selection + ")!");
            cm.dispose();
        }
    } else if (status == 3) {
        this.itemChoice = this.sub[selection];
        cm.sendNext("Ths item costs #b" + StringUtil.formatNumber(this.itemChoice[2]) + " " + display + "#k, are you sure you want to buy this?\r\n\r\n\t\t#v" + this.itemChoice[0] + "# #b#z" + this.itemChoice[0] + "#");
    } else if (status == 4) {
        if (getPoints(pointsType) >= this.itemChoice[2]) {
            if (InventoryModifier.checkSpace(client, this.itemChoice[0], this.itemChoice[1], "")) {

                var log = player.getName() + " traded " + this.itemChoice[2] + " " + display + " for " + this.itemChoice[1] + " of item " + this.itemChoice[0];
                var transactionId = createTransaction(cm.getDatabaseConnection(), player.getId(), log);
                if (transactionId == -1) {
                    print("Error creating transaction log...");
                    print(log);
                }

                if (gainPoints(pointsType, -this.itemChoice[2])) {
                    cm.gainItem(this.itemChoice[0], this.itemChoice[1], true);
                    cm.sendOk("Thank you for your purchase!\r\nYou now have #b" + getPoints(pointsType) + " " + display + "\r\n#kHere is your transaction ID : #b" + transactionId);
                } else {
                    cm.sendOk("I'm sorry! Something wrong must have happened, I apparently don't accept the " + display + " currency. Please let an Administrator know about this ASAP!");
                }
            } else {
                cm.sendOk("You do not have enough #b" + ItemConstants.getInventoryType(this.itemChoice[0]).name() + "#k inventory space to purchase this item");
            }
        } else {
            cm.sendOk("You don't have enough #b" + display + "#k to make this purchase");
        }
        cm.dispose();
    }
}

function getPoints(s) {
    if (typeof s == 'number') {
        return cm.itemQuantity(s);
    }
    switch(s) {
        default: return -1;
        case "PQ points": return player.getPartyQuestPoints();
        case "donor points": return client.getDonationPoints();
        case "fishing points": return player.getFishingPoints();
        case "event points": return player.getEventPoints();
        case "vote points": return client.getVotePoints();
        case "easy points": return player.getEasyBossPoints();
        case "normal points": return player.getNormalBossPoints();
        case "hard points": return player.getHardBossPoints();
        case "hell points": return player.getHellBossPoints();
        case "NX cash": return player.getCashShop().getCash(1);
    }
}

function gainPoints(s, amt) {
    if (typeof s == 'number') {
        cm.gainItem(s, amt, true);
        return true;
    }
    switch (s) {
        default: return false;
        case "PQ points":
            player.addPoints("pq", amt);
            return true;
        case "event points":
            player.addPoints("ep", amt);
            return true;
        case "donor points":
            player.addPoints("dp", amt);
            return true;
        case "fishing points":
            player.addPoints("fp", amt);
            return true;
        case "vote points":
            player.addPoints("vp", amt);
            return true;
        case "easy points":
            player.addPoints("easy", amt);
            return true;
        case "normal points":
            player.addPoints("normal", amt);
            return true;
        case "hard points":
            player.addPoints("hard", amt);
            return true;
        case "hell points":
            player.addPoints("hell", amt);
            return true;
        case "NX cash":
            player.getCashShop().gainCash(1, amt);
            return true;
    }
}
