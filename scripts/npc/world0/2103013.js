const EntranceFieldID = 926010000; // entrance map
const ExitFieldID = 926020001; // completion map
const ItemGems = [4001322, 4001323, 4001324, 4001325];
/* izarooni */
let status = 0;

function action(mode, type, selection) {
	if (mode < 1) {
		cm.dispose();
		return;
	} else {
		status++;
	}
	if (cm.getMapId() == EntranceFieldID) return EnterPyramid(selection);
	else if (cm.getMapId() == ExitFieldID) return CompletePyramid(selection);
	else return ForfeitPyramid(selection);
}

function EnterPyramid(selection) {
	if (status == 1) {
		cm.sendSimple("I am Duarte.\r\n"
		+ "\r\n#b#L0# Ask about the Pyramid.#l"
		+ "\r\n#e#L1# Enter the Pyramid.#l#n\r\n"
		+ "\r\n#L2# Enter Pharaoh Yeti's Tomb.#l"
		+ "\r\n#L3# Ask about Pharaoh Yeti's treasures.#l");
	} else if (status == 2) {
		cm.vars = { selection: selection };
		if (selection == 0) {
			cm.sendNext("This is the pyramid of Nett, the god of chaos and revenge. For a long time, it was buried deep in the desert, but Nett has ordered it to rise above ground. If you are unafraid of chaos and possible death, you may challenge Pharaoh Yeti, who lies asleep inside the Pyramid. Whatever the outcome, the choice is yours to make.");
		} else if (selection == 1) { // enter
			cm.sendSimple("You fools who know no fear of Nett's wrath, it is now time to choose your destiny!\r\n" 
			+ "\r\n#b#L0# Enter alone.#l" 
			+ "\r\n#L1# Enter with a party of 2 or more.#l");
		} else if (selection == 2) { // yeti tomb
			let content = "What gem have you b rought?\r\n";
			for (let i = 0; i < ItemGems.length; i++) {
				let ItemID = ItemGems[i];
				content += `\r\n#L${ItemID}##i${ItemID}# #t${ItemID}##l`;
			}
			cm.sendSimple(content);
		} else if (selection == 3) {
			cm.sendNext("Inside Pharaoh Yeti's Tomb, you can acquire a #e#b#t2022613##k#n by proving yourself capable of defeating the #bPharaoh Jr. Yeti#k, the Pharaoh's clone. Inside that box lies a very special treasure. It is the #e#b#t1132012##k#n.\r\n#i1132012:# #t1132012#\r\n\r\n And if you are somehow able to survive Hell Mode, you will receive the #e#b#t1132013##k#n.\r\n\r\n#i1132013:# #t1132013#\r\n\r\n Though, of course, Nett won't allow that to happen.");
			status = 0;
		}
	} else if (status == 3) {
		selection = cm.vars.selection;
		if (selection == 0) {
			cm.sendNextPrev("Once you enter the Pyramid, you will be faced with the wrath of Nett. Since you don't look too sharp, I will offer you some advice and rules to follow. Remember them well.#b\r\n\r\n1. Be careful that your #e#rAct Gauge#b#n does not decrease. The only way to maintain your Gauge level is to battle the monsters without stopping.\r\n2. Those who are unable will pay dearly. Be careful to not cause any #rMiss#b.\r\n3. Be wary of the Pharaoh Jr. Yeti with the #v04032424# mark. Make the mistake of attacking him and you will regret it.\r\n4. Be wise about using the skill that is given to you for Kill accomplishments.");
		} else if (selection == 1) { // enter
			cm.sendSimple("You who lack fear of death's cruelty, make your decision!\r\n#L0##i3994115##l#L1##i3994116##l#L2##i3994117##l#L3##i3994118##l");
		} else if(selection == 2) { // tomb
			if (!cm.haveItem(selection)) {
				cm.sendOk("You'll need a gem to enter Pharaoh Yeti's Tomb. Are you sure you have one?");
				cm.dispose();
			} else {
				cm.sendOk("TEST");
			}
		}
	} else if (status == 4) {
		selection = cm.vars.selection;
		if (selection == 0) {
			cm.sendNextPrev("Those who are able to withstand Nett's wrath will be honored, but those who fail will face destruction. This is all the advice I can give you. The rest is in your hands.");
			status = 0;
		} else if (selection == 1) { // enter
			let em = cm.getEventManager("NettPyramid");
			if (em == null) {
				cm.sendOk("Seems Nett's Pyramid is under construction right now. Check again later");
				return cm.dispose();
			}
			let eim = em.getInvocable().invokeFunction("setup");
			eim.vars.difficulty = selection;
			eim.registerPlayer(player);
			cm.dispose();
		}
	}
}

function CompletePyramid() {
	if (status == 1) {
		cm.sendSimple("Stop! You've succesfully passed Nett's test. By Nett's grace, you will now be given the opportunity to enter Pharaoh Yeti's Tomb. Do you wish to enter it now?\r\n\r\n#b#L0# Yes, I will go now.#l\r\n#L1# No, I will go later.#l");
	} else if (status == 2) {
		if (selection == 0) {
			cm.dispose();
		} else if (selection == 1) {
			cm.sendNext("I will give you Pharaoh Yeti's Gem. You will be able to enter Pharaoh Yeti's Tomb anytime with this Gem. Check to see if you have at least 1 empty slot in your Etc window.");
		}
	} else if (status == 3) {
		if (cm.canHold(givingItem)) {
			let givingItem = 4001325;
			if (player.getLevel() >= 60) givingItem = 4001325;
			cm.gainItem(givingItem);
			cm.warp(EntranceFieldID)
		}
		cm.dispose();
	}
}

function ForfeitPyramid(selection) {
	if (status == 1) {
		cm.sendSimple("Do you want to forfeit the challenge and leave?\r\n\r\n#b#L0# Leave#l");
	} else if (status == 2) {
		let eim = player.getEventInstance();
		if (eim != null) {
			eim.removePlayer(player);
		} else {
			cm.warp(EntranceFieldID);
		}
		cm.dispose();
	}
}