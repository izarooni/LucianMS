/* original script by izarooni */
/* script modified by kerrigan */

load("scripts/util_imports.js");
load("scripts/util_transaction.js");

// Format: [ItemID cost quantity]
const item_master = {
  "Equipment Sets": {
    "Crimson Maple Set": "[1003243 50 1],[1052358 50 1],[1102295 50 1],[1082315 50 1],[1132093 50 1],[1072522 50 1]",
    "Legendary Maple Set": "[1003364 100 1],[1052405 100 1],[1102322 100 1],[1082391 100 1],[1132110 100 1],[1072610 100 1]",
    "Grand Maple Amythesian Set": "[1003529 300 1],[1052457 300 1],[1102394 300 1],[1082430 300 1],[1072660 300 1]"
  },
  "Medals": "[1142822 5 1],[1142704 10 1],[1142767 50 1],[1142866 100 1],[1142145 200 1],[1142438 400 1]",
  "Usable Items": "[2049100 80 1],[2340000 80 1],[3018375 500 1],[1005389 100 1],[1005390 100 1],[1115085 200 1],[1115174 200 1]"
};

let status = 0;
var currency = 4011024;
var prev_select = -1;

const dialog = {
    first: "Welcome to the #dArcade#k shop. Here you can exchange \r\n#barcade coins#k for various equips and usable items. You currently have #b" + getPoints(currency) + " arcade coins#k. What would you like to buy?\r\n#b",
    set: "Certainly. Which set would you like to view?\r\n#b",
    display: "Here are all of the #d[itemtype]#k you can currently buy\r\n#b",
    setdisplay: "Here are the items in the #d[setname]#k:\r\n#b",
};

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
        status ++;
    }
    if (status == 0) {
        let text = dialog.first;
        var i = 0;
        for (var it in item_master) {
            text += "\r\n#L" + (i++) + "#" + it + "#l";
        }
        cm.sendSimple(text);
    }
    else if (status == 1) {
        this.picked = item_master[Object.keys(item_master)[selection]];
        prev_select = selection;
        if (typeof this.picked === 'object') {
          var text = dialog.set;
          var i = 0;
          for (var j in this.picked) {
            text += "\r\n#L" + (i++) + "#" + j + "#l";
          }
         cm.sendSimple(text);
        }
        else {
          var text = dialog.display.replace("[itemtype]", Object.keys(item_master)[selection])
          this.item_list = parser(this.picked);
          for (var k in this.item_list) {
            var i = 0;
            let itemID = item_list[k][0];
            text += "\r\n#L" + k + "#" + "#v" + itemID + "#  #z" + itemID + "##l";
          }
          cm.sendSimple(text + '\r\n');
        }
    }
    else if (status == 2) {
        if (typeof item_master[Object.keys(item_master)[prev_select]] == 'object') {
          var text = dialog.setdisplay.replace("[setname]", Object.keys(this.picked)[selection]);
          this.picked_2 = this.picked[Object.keys(this.picked)[selection]];
          this.item_list = parser(this.picked_2);
          for (var k in this.item_list) {
            let itemID = this.item_list[k][0];
            text += "\r\n#L" + k + "#" + "#v" + itemID + "#  #z" + itemID + "##l";
          }
          cm.sendSimple(text);
        } else {
            this.itemChoice = this.item_list[selection];
            cm.sendYesNo("#d#z" + this.itemChoice[0] + "# #kcosts #b" + StringUtil.formatNumber(itemChoice[1]) + " arcade coin(s)#k, would you like to make this purchase?\r\n\r\n#v" + itemChoice[0] + "#  #d  #z" + itemChoice[0] + "#");
        }
    }
    else if (status == 3) {
        if (typeof item_master[Object.keys(item_master)[prev_select]] == 'object') {
          this.itemChoice = this.item_list[selection];
          cm.sendYesNo("#d#z" + this.itemChoice[0] + "# #kcosts #b" + StringUtil.formatNumber(itemChoice[1]) + " arcade coin(s)#k, would you like to make this purchase?\r\n\r\n#v" + itemChoice[0] + "#  #d  #z" + itemChoice[0] + "#");
        }
        else {
          buyItem(this.itemChoice);
        }
    }
    else if (status == 4) {
        if (typeof item_master[Object.keys(item_master)[prev_select]] == 'object') {
          buyItem(this.itemChoice);
        }
        else {
          cm.dispose();
        }
    }
}

function getPoints(s) {
   return cm.itemQuantity(s);
}

function gainPoints(s, amt) {
    if (typeof s == 'number') {
        cm.gainItem(s, amt, true);
        return true;
    }
}

function buyItem(input) {
  if (getPoints(currency) >= input[1]) {
      if (InventoryModifier.checkSpace(client, input[0], input[1], "")) {
          var log = player.getName() + " traded " + input[1] + " arcade coins for " + input[2] + " of item " + input[0];
          var transactionId = createTransaction(cm.getDatabaseConnection(), player.getId(), log);
          if (transactionId == -1) {
              print("Error creating transaction log...");
              print(log);
          }

          if (gainPoints(currency, -input[1])) {
              cm.gainItem(input[0], input[2], true);
              cm.sendOk("Thank you for buying #d#z " + input[0] + "##k!\r\nYou now have #b" + getPoints(currency) + " arcade coins#k.\r\n#kTransaction ID : #b" + transactionId);
          } else {
              cm.sendOk("I'm sorry! Something wrong must have happened, I apparently don't accept arcade coins. Please contact a GM about this ASAP!");
          }
      } else {
          cm.sendOk("You do not have enough #d" + ItemConstants.getInventoryType(input[0]).name() + "#k inventory space to purchase this item");
      }
  }
  else {
      cm.sendOk("You don't have enough #barcade coins#k to make this purchase");
  }
}

function parser(input) {
  var out = [];
  var item_array = input.split(',');
  for (var i in item_array) {
    out.push(item_array[i].replace(/[\[\]']+/g, '').split(" "));
  }
  return out;
}
