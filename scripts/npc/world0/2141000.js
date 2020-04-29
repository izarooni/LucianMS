//kerrigan

load("scripts/util_imports.js");
load("scripts/util_transaction.js");

/* Format:
		Craftable -> items to be crafted
		Cost -> items required to craft item, sequential (first set of brackets in cost corresponds to the first craftable item id)
		Quantity -> required quantities of each cost item, sequential (first set of brackets in quantity corresponds to the first set of brackets in cost)
*/

const item_master = {
  "Craftable": "[1004808],[1102940],[1082695],[1053063],[1073158],[1302343],[1312203],[1322255],[1402259],[1412181],[1422189],[1432218],[1442274],[1004809],[1102941],[1082696],[1053064],[1073159],[1372228],[1382265],[1004810],[1102942],[1082697],[1053065],[1073160],[1452257],[1462243],[1004812],[1102944],[1082699],[1053067],[1073162],[1492235],[1482221],[1004811],[1102943],[1082698],[1053066],[1073161],[1472265],[1332279 ]",
  "Cost": "[4011506 4000313],[4011506 4000313],[4011506 4000313],[4011506 4000313],[4011506 4000313],[4011506 4000313],[4011506 4000313],[4011506 4000313],[4011506 4000313],[4011506 4000313],[4011506 4000313],[4011506 4000313],[4011506 4000313],[4011506 4000313],[4011506 4000313],[4011506 4000313],[4011506 4000313],[4011506 4000313],[4011506 4000313],[4011506 4000313],[4011506 4000313],[4011506 4000313],[4011506 4000313],[4011506 4000313],[4011506 4000313],[4011506 4000313],[4011506 4000313],[4011506 4000313],[4011506 4000313],[4011506 4000313],[4011506 4000313],[4011506 4000313],[4011506 4000313],[4011506 4000313],[4011506 4000313],[4011506 4000313],[4011506 4000313],[4011506 4000313],[4011506 4000313]",
  "Quantity": "[200 100],[150 150],[200 100],[250 200],[200 100],[300 200],[300 200],[300 200],[300 200],[300 200],[300 200],[300 200],[300 200],[200 100],[150 150],[200 100],[250 200],[200 100],[300 200],[300 200],[200 100],[150 150],[200 100],[250 200],[200 100],[300 200],[300 200],[200 100],[150 150],[200 100],[250 200],[200 100],[300 200],[300 200],[200 100],[150 150],[200 100],[250 200],[200 100],[300 200],[300 200]"
};


let status = 0;
var prev_select = -1;
var item_name = "";

const dialog = {
    first: "Are you interested in powerful equipment? I have what you are seeking.",
    tutorial: "#b#z4011506##k drops from monsters located at the #eArcane River#n.\r\n\r\nEach monster there has a chance of dropping #b#z4011506##kand if you provide me enough, I will with my magic create you powerful equips.\r\n\r\nIf you are interested in earning bonus #b#z4011506##k then talk to #eDame Knight#n. She might have some #especial#n quests that rewards #b#z4011506##k.",
    second: "What do you seek?\r\n#b",
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
		  text += "\r\n#d#L0#What are Arcane stars?\r\n#L1#Show me a list of items I can craft.";
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
