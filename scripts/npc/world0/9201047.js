/* original script by izarooni */
/* script modified by kerrigan */

load("scripts/util_imports.js");
load("scripts/util_transaction.js");

// Format: [ItemID cost quantity]
const item_master = {
  //"Android Set": "[1003108 1 1],[1042191 1 1],[1082275 1 1],[1062125 1 1],[1072446 1 1]",
  "Unwelcome Guest Set": {
    "1st Unwelcome Guest Set": "[1302143 1 1],[1312058 1 1],[1322086 1 1],[1332116 1 1],[1332121 1 1],[1372074 1 1],[1382095 1 1],[1402086 1 1],[1412058 1 1],[1422059 1 1],[1432077 1 1],[1442107 1 1],[1452102 1 1],[1462087 1 1],[1472113 1 1],[1482075 1 1],[1492075 1 1]",
    "2nd Unwelcome Guest Set": "[1302144 1 1],[1312059 1 1],[1322087 1 1],[1332117 1 1],[1332122 1 1],[1372075 1 1],[1382096 1 1],[1402087 1 1],[1412059 1 1],[1422060 1 1],[1432078 1 1],[1442108 1 1],[1452103 1 1],[1462088 1 1],[1472114 1 1],[1482076 1 1],[1492076 1 1]",
    "3rd Unwelcome Guest Set": "[1302145 1 1],[1312060 1 1],[1322088 1 1],[1332118 1 1],[1332123 1 1],[1372076 1 1],[1382097 1 1],[1402088 1 1],[1412060 1 1],[1422061 1 1],[1432079 1 1],[1442109 1 1],[1452104 1 1],[1462089 1 1],[1472115 1 1],[1482077 1 1],[1492077 1 1]",
    "Last Unwelcome Guest Set": "[1302146 1 1],[1312061 1 1],[1322089 1 1],[1332119 1 1],[1332124 1 1],[1372077 1 1],[1382098 1 1],[1402087 1 1],[1412061 1 1],[1422062 1 1],[1432080 1 1],[1442110 1 1],[1452105 1 1],[1462090 1 1],[1472116 1 1],[1482078 1 1],[1492078 1 1]"
  },
  "Visitor Set": {
    "Powerful Visitor Set": "[1003116 3 1],[1052278 3 1],[1082278 3 1],[1072450 3 1]",
    "Dextrous Visitor Set": "[1003118 1 1],[1052280 1 1],[1082280 1 1],[1072452 1 1]",
    "Wise Visitor Set": "[1003117 2 1],[1052279 2 1],[1082279 2 1],[1072451 2 1]",
    "Lucky Visitor Set": "[1003119 4 1],[1052281 4 1],[1082281 4 1],[1072453 4 1]"
  }
}



let status = 0;
var currency = 4260002;
var prev_select = -1;
var display = "#z" + currency + "#";

const dialog = {
    first: "Hello! I sell #dUnwelcome Guest#k, and #dVisitor #kset items. You currently have #b" + getPoints(currency) + " " + display + ". #kWhich set would you like to view?\r\n#b",
    subset: "The #d[setname]#k has [length] varities. Which subset would you like to view?:\r\n#b",
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
          var text = dialog.subset.replace("[setname]", Object.keys(item_master)[prev_select]);
          text = text.replace("[length]", Object.keys(this.picked).length);
          var i = 0;
          for (var j in this.picked) {
            text += "\r\n#L" + (i++) + "#" + j + "#l";
          }
         cm.sendSimple(text);
        }
        else {
          var text = dialog.setdisplay.replace("[setname]", Object.keys(item_master)[selection]);
          this.item_list = parser(this.picked);
          for (var k in item_list) {
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
          //var picked = item_master[Object.keys(item_master)[prev_select]];
          this.picked_2 = this.picked[Object.keys(this.picked)[selection]];
          this.item_list = parser(this.picked_2);
          for (var k in this.item_list) {
            let itemID = this.item_list[k][0];
            text += "\r\n#L" + k + "#" + "#v" + itemID + "#  #z" + itemID + "##l";
          }
          cm.sendSimple(text);
        } else {
            this.itemChoice = this.item_list[selection];
            cm.sendYesNo("#d#z" + this.itemChoice[0] + "# #kcosts #b" + StringUtil.formatNumber(itemChoice[1]) + " " + display + "#k, would you like to make this purchase?\r\n\r\n#v" + itemChoice[0] + "#  #d  #z" + itemChoice[0] + "#");
        }
    }
    else if (status == 3) {
        if (typeof item_master[Object.keys(item_master)[prev_select]] == 'object') {
          this.itemChoice = this.item_list[selection];
          cm.sendYesNo("#d#z" + this.itemChoice[0] + "# #kcosts #b" + StringUtil.formatNumber(itemChoice[1]) + " " + display + "#k, would you like to make this purchase?\r\n\r\n#v" + itemChoice[0] + "#  #d  #z" + itemChoice[0] + "#");
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
          var log = player.getName() + " traded " + input[2] + " " + display + " for " + input[1] + " of item " + input[0];
          var transactionId = createTransaction(cm.getDatabaseConnection(), player.getId(), log);
          if (transactionId == -1) {
              print("Error creating transaction log...");
              print(log);
          }

          if (gainPoints(currency, -input[1])) {
              cm.gainItem(input[0], input[2], true);
              cm.sendOk("Thank you for buying #d#z " + input[0] + "##k!\r\nYou now have #b" + getPoints(currency) + "#b " + display + "#k.\r\n#kTransaction ID : #b" + transactionId);
          } else {
              cm.sendOk("I'm sorry! Something wrong must have happened, I apparently don't accept the " + display + " currency. Please contact a GM about this ASAP!");
          }
      } else {
          cm.sendOk("You do not have enough #d" + ItemConstants.getInventoryType(input[0]).name() + "#k inventory space to purchase this item");
      }
  }
  else {
      cm.sendOk("You don't have enough #b" + display + "#k to make this purchase");
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
