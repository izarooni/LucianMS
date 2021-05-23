//kerrigan

load("scripts/util_imports.js");
load("scripts/util_transaction.js");

/* Format:
		Craftable -> items to be crafted
		Cost -> items required to craft item, sequential (first set of brackets in cost corresponds to the first craftable item id)
		Quantity -> required quantities of each cost item, sequential (first set of brackets in quantity corresponds to the first set of brackets in cost)
*/

const item_master = {
  "Craftable": "[4011503],[4011022],[1113144],[4011508],[4011509],[4011510],[1003112],[1004637],[1122076],[1032222],[1122266],[1132245],[1113074],[1032223],[1122267],[1132246],[1113075]",
  "Cost": "[4011047 4011048 4011049 4011050 4011051 4011502],[4011504 4011505],[4011503],[4260002],[2340000],[2049100],[1002357 2340000 2049100],[1002357 1003112 2340000 2049100],[1122000 2340000 2049100],[2340000 2049100 5220010],[2340000 2049100 5220010],[2340000 2049100 5220010],[2340000 2049100 5220010],[2340000 2049100 5220010 5220020 1032222],[2340000 2049100 5220010 5220020 1122266],[2340000 2049100 5220010 5220020 1132245],[2340000 2049100 5220010 5220020 1113074]",
  "Quantity": "[1 1 1 1 1 1],[1 1],[1],[1],[150],[100],[1 100 50],[1 1 50 20],[1 150 100],[100 150 100],[150 200 50],[100 100 100],[50 100 20],[100 200 50 50 1],[200 200 50 50 1],[100 100 100 100 1],[100 150 50 50 1]"
};


let status = 0;
var prev_select = -1;
var item_name = "";

const dialog = {
    first: "When you're done saving, come over here, kupo! I'll tell you about the Moogle Shop and Item Synthesis, kupo.",
    tutorial: "Kupo. You are silly! You gather me materials and I will synthesis something good for ya, kupo!\r\n\r\nNow, kupo. There is no complain at my store! Okay? Kupo.\r\n\r\n#bHow do I obtain the Lucid crystal pieces?#k\r\nThe Lucid crystal pieces are hidden all over the #rChirithy world.#k\r\nSome are obtainable by a custom quest, maybe a custom mini boss or perhaps the mysterious wishing dragon?\r\nYou better figure out, kupo!\r\n#b",
    second: "Here, kupo! These are all my goods you can select from! kupo.\r\n#b",
    craft: "A #d#z[item]##k requires the following to craft:\r\n#b"
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
		  text += "\r\n#d#L0#How does this work?\r\n#L1#Show me a list of items I can craft.";
      cm.sendSimple(text);
    }
    else if (status == 1) {
				if (selection == 0) {
					cm.sendOk(dialog.tutorial);
          cm.dispose();
				}
				else if (selection == 1) {
					let text = dialog.second;
					item_list = item_parser(item_master[Object.keys(item_master)[0]]);
					for (item in item_list) {
						text += "\r\n#L" + item + "##v"+ item_list[item] + "#       #z" + item_list[item] + "#\r\n";
					}
					cm.sendSimple(text);
      	}
		}
		else if (status == 2) {
			prev_select = selection;
			item_name = item_list[prev_select];
			var text = dialog.craft.replace("[item]", item_name);
			cost_list = parser(item_master[Object.keys(item_master)[1]])[selection];
			quantity_list = parser(item_master[Object.keys(item_master)[2]])[selection];
			for (i in cost_list) {
				text += "\r\n#d#v" + cost_list[i] + "#       #z" + cost_list[i] + "#       " + getPoints(cost_list[i]) + " / " + quantity_list[i] + "\r\n";
			}
			text += "\r\nWould you like to craft a #z" + item_name + "#?\r\n";
			cm.sendYesNo(text);
		}
		else if (status == 3) {
      if (checkCraftable(cost_list, quantity_list)) {
        for (i in cost_list) {
  				cm.gainItem(parseInt(cost_list[i]), -1 * parseInt(quantity_list[i]), true);
  			}
        cm.gainItem(parseInt(item_name), 1, true);
        cm.sendOk("You have obtained a #z" + item_name + "#.");
      }
      else {
        cm.sendOk("You appear to be missing some of the items required to craft this.");
      }
   }
}
function getPoints(s) {
   return cm.itemQuantity(s);
}

function item_parser(input) {
	var out = [];
	var item_array = input.split(',');
	for (var i in item_array) {
		out.push(item_array[i].replace(/[\[\]']+/g, ''));
	}
	return out;
}

function parser(input) {
  var out = [];
  var item_array = input.split(',');
  for (var i in item_array) {
    out.push(item_array[i].replace(/[\[\]']+/g, '').split(" "));
  }
  return out;
}

function checkCraftable(cost, quantity) {
  counter = 0;
  for (i in cost) {
    if (cm.itemQuantity(parseInt(cost[i])) >= parseInt(quantity[i])) {
      counter ++;
    }
  }
  if (counter >= quantity.length) {
    return true;
  } else {
    return false;
  }
}
