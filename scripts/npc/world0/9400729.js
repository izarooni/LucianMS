load('scripts/util_imports.js');
load("scripts/util_transaction.js");
/* izarooni */
let status = 0;
let selections = [
	[1000000000, [1, 4260002]],
	[500000000, [1, 4011022]]
];
let playerChoice;

function action(mode, type, selection) {
	if (mode < 1) {
		cm.dispose();
		return;
	} else {
		status++;
	}
	if (status == 1) {
		let content = "Are you interested in any of my offers?\r\n#b";
		for (let i = 0; i < selections.length; i++) {
			let cost = selections[i][0];
			let offer = selections[i][1];
			let readable = StringUtil.formatNumber(cost);
			content += `\r\n#L${i}#${readable} mesos for ${offer[0]} #z${offer[1]}##l`;
		}
		cm.sendSimple(content);
	} else if (status == 2) {
		playerChoice = selections[selection];
		let cost = StringUtil.formatNumber(playerChoice[0]);
		let offer = playerChoice[1];
		cm.sendNext(`Are you sure you want to trade\r\n#b${cost}#k mesos for ${offer[0]} #b#z${offer[1]}##k?`);
	} else if (status == 3) {
		let cost = playerChoice[0];
		let offer = playerChoice[1];
		if (player.getMeso() >= cost) {
			if (InventoryModifier.checkSpace(client, offer[1], offer[0], "")) {
				cm.gainItem(offer[1], offer[0], true);
				cm.gainMeso(-cost);

				let log = `${player.getName()} traded ${cost} mesos for ${offer[0]} of ${offer[1]}`;
				let transactionId = createTransaction(cm.getDatabaseConnection(), player.getId(), log);
				if (transactionId == -1) {
					print("Error creating transaction log...");
					print(log);
				}

				cm.sendOk("Enjoy!\r\n#kHere is your transaction ID : #b" + transactionId)
				cm.dispose();
			} else {
				cm.sendOk("Please make space in your inventory before making trades!");
			}
		} else {
			cm.sendOk("You do not have enough mesos to make this purchase");
		}
		cm.dispose();
	}
}