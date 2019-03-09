load('scripts/util_gpq.js');
const MapleMapItem = Java.type('com.lucianms.server.maps.MapleMapItem');
/* izarooni 
Guardian Statue - Sharenian: Fountain of the Wiseman (990000500)
*/
let status = 0;

function action(mode, type, selection) {
	if (mode < 1) {
		cm.dispose();
		return;
	} else {
		status++;
	}
	let eim = player.getEventInstance();
	if (eim == null) {
		if (status == 1) cm.sendNext("What are you doing here?");
		else {
			cm.wrap(nFieldEnding);
			cm.dispose();
		}
		return;
	}
	if (eim.isLeader(player)) {
		let gate = player.getMap().getReactorByName("watergate");
		if (gate.getState() == 1) {
			cm.sendOk("I have already opened the passage to waterway for you.");
			cm.dispose();
		} else return GiveRiddle();
	} else {
		cm.sendOk("I will only speak to the party leader.");
		cm.dispose();
	}
}

function GiveRiddle() {
	let eim = player.getEventInstance();
	if (player.isDebug() || eim.vars.wisemanCombo == undefined) {
		eim.vars.wisemanCombo = CreateCombo();
		if (eim.vars.debug) {
			player.sendMessage(6, `[DEBUG_MODE] Created combo: ${eim.vars.wisemanCombo}`);
			if (player.isDebug()) {
				let areas = player.getMap().getAreas();
				for (let i = 0; i < 4; i++) {
					
				}
			}
		}
		cm.sendOk("This fountain guards the secret passage to the throne room. Offer items in the area to the vassals to proceed. The vassals shall tell you whether your offerings are accepted, and if not, which vassals are displeased. You have seven attempts. Good luck.");
		return cm.dispose();
	}

	let desired = eim.vars.wisemanCombo;
	let combo = getCombo();
	if (combo == undefined) return;
	if (eim.vars.debug) print(`[GPQ Wiseman (9040006.js)]\r\n\tEntered: ${combo}\r\n\tDesired: ${desired}`);

	let attempt = nthNumber(eim.vars.wisemanAttempt);
	let matches = { correct: 0, incorrect: 0, unknown: 0 };
	for (let i = 0; i < 4; i++) {
		if (desired.indexOf(combo[i]) < 0) matches.unknown++;
		else if (desired[i] != combo[i]) matches.incorrect++;
		else matches.correct++;
	}
	if (matches.correct != 4) {
		if (eim.vars.wisemanAttempt < 7) {
			let content = "";
			if (matches.correct > 0) content += `${matches.correct} agreed that the offering is correct.\r\n`;
			if (matches.incorrect > 0) content += `${matches.incorrect} have declared the offering is incorrect.\r\n`;
			if (content.unknown > 0) content += `${matches.unknown} have said it 's an unknown offering.\r\n`;
			content += `This is your ${attempt} attempt`;
			cm.sendOk(content);
			eim.vars.wisemanAttempt++;
		} else {
			eim.vars.wisemanAttempt = 1;
			eim.vars.wisemanCombo = undefined;
			cm.sendOk("You have failed the test. Please compose yourselves and try again later.");
			for (var i = 0; i < 6; i++) {
				cm.spawnMonster(9300036, getRandomXPosition(), 150);
				cm.spawnMonster(9300037, getRandomXPosition(), 150);
			}
			cm.dispose();
		}
	} else {
		let gate = player.getMap().getReactorByName("watergate");
		player.getMap().clearDrops();
		if (gate.getState() == 0) gate.hitReactor(client);
		cm.getGuild().gainGP(25);
		cm.sendOk("You may proceed");
	}
	cm.dispose();
}

function CreateCombo() {
	let combo = [];
	for (let i = 0; i < 4; i++)
		combo.push(nItemValorMedal + Math.floor(Math.random() * 4));
	return combo;
}

function getCombo() {
	let map = player.getMap();

	let drops = map.getMapObjects(MapleMapItem.class);
	if (drops.size() > 4) {
		player.sendMessage(5, "You have offered too many items to the vassals.");
		drops.clear();
		return cm.dispose();
	} else if (drops.size() != 4) {
		player.sendMessage(5, "You have not offered enough items to the vassals.");
		drops.clear();
		return cm.dispose();
	}

	let offerings = [];
	let iter = drops.iterator();
	while (iter.hasNext()) {
		let drop = iter.next();
		let itemID = drop.getItemId();
		if (!(itemID >= nItemValorMedal && itemID <= nItemNeckiDrink)) {
			cm.sendOk("There are undesired offerings present.");
			break;
		}
		for (let i = 0; i < 4; i++) { // for-each area in the map
			if (map.getArea(i).contains(drop.getPosition())) {
				offerings[i] = drop.getItemId();
			}
		}
	}
	drops.clear();
	return offerings;
}

function nthNumber(n) {
	if (n == 0) return `${n}th`;
	else if (n == 1) return `${n}st`;
	else if (n == 2) return `${n}nd`;
	else if (n == 3) return `${n}rd`;
	else return `${n}th`;
}

function getRandomXPosition() {
	return -350 + Math.floor(Math.random() * 750);
}