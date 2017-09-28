load("scripts/util_imports.js");
load("scripts/util_transaction.js");
/* izarooni */
var status = 0;
var broken = null;
var items = {
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
    if (status == 1) {
        if (broken != null) {
            if (player.isGM()) {
                cm.sendOk("Hey #h #, I see you're a GM. An error occurred while parsing the " + pointsType + " trader data. Please let a Developer know ASAP!\r\n#r" + broken);
            } else {
                cm.sendOk("Someone stole my shop items, I have nothing to sell!\r\nPlease report me to an Administrator ASAP so I can get my shop open once again");
            }
            cm.dispose();
            return;
        }
        var text = "Hey #h #! You currently have #b" + getPoints(pointsType) + " " + pointsType + "#k\r\nIs there anything you would like to buy?\r\n#b";
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
        if (this.sub != null) {
            var subName = items.get(selection, false);
            var text = "Absolutely, what item interests you?\r\n#b";
            for (var i = 0; i < this.sub.length; i++) {
                if (subName == "Chairs") {
                    // a lot of custom chairs don't have names, thus viewing the item in shop via name is impossible.
                    // instead, showing the image of the item (although small) is much better than showing nothing
                    text += "#L" + i + "##v" + this.sub[i][0] + "##l";
                    if (i % 5 == 0 && i > 0) text += "\r\n";
                } else {
                    text += "\r\n#L" + i + "##z" + this.sub[i][0] + "##l";
                }
            }
            cm.sendSimple(text);
        } else {
            cm.sendOk("Error(" + selection + ")!");
            cm.dispose();
        }
    } else if (status == 3) {
        this.itemChoice = this.sub[selection];
        cm.sendNext("Are you sure you want to buy this item for #b" + this.itemChoice[2] + " " + pointsType + "#k?\r\n#v" + this.itemChoice[0] + "##b#z" + this.itemChoice[0] + "#");
    } else if (status == 4) {
        if (getPoints(pointsType) >= this.itemChoice[2]) {
            if (InventoryModifier.checkSpace(client, this.itemChoice[0], this.itemChoice[1], "")) {

                var log = player.getName() + " traded " + this.itemChoice[2] + " " + pointsType + " for " + this.itemChoice[1] + " of item " + this.itemChoice[0];
                var transactionId = createTransaction(player.getId(), log);
                if (transactionId == -1) {
                    print("Error creating transaction log...");
                    print(log);
                }

                if (gainPoints(pointsType, -this.itemChoice[2])) {
                    cm.gainItem(this.itemChoice[0], this.itemChoice[1], true);
                    cm.sendOk("Thank you for your purchase!\r\nYou now have #b" + getPoints(pointsType) + " " + pointsType + "\r\n#kHere is your transaction ID : #b" + transactionId);
                } else {
                    cm.sendOk("I'm sorry! Something wrong must have happened, I apparently don't accept the " + pointsType + " currency. Please let an Administrator know about this ASAP!");
                }
            } else {
                cm.sendOk("You do not have enough #b" + ItemConstants.getInventoryType(this.itemChoice[0]).name() + "#k inventory space to purchase this item");
            }
        } else {
            cm.sendOk("You don't have enough #b" + pointsType + "#k to make this purchase");
        }
        cm.dispose();
    }
}

function getPoints(s) {
    switch(s) {
        default: return -1;
        case "donor points": return client.getDonationPoints();
        case "fishing points": return player.getFishingPoints();
        case "event points": return player.getEventPoints();
        case "vote points": return client.getVotePoints();
    }
}

function gainPoints(s, amt) {
    switch (s) {
        default: return false;
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
    }
}