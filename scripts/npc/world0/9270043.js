var common = Array(1008545, 1008546, 1008551, 1011007, 1902015, 1103019);


function getRandom(min, max) {
	if (min > max) {
		return(-1);
	}

	if (min == max) {
		return(min);
	}

	return(min + parseInt(Math.random() * (max - min + 1)));
}

var icommon = common[getRandom(0, common.length - 1)];

var chance = getRandom(0, 5);

function start() {
	status = -1;
	action(1, 0, 0);
}

function action(mode, type, selection) {
	if (mode == -1) {
		cm.dispose();
	} else {
		if (mode == 0) {
			cm.dispose();
			return;
		}
		if (mode == 1)
			status++;
		else
			status--;
		if (status == 0) {
			cm.sendSimple("Do you have a#r Overwatch Loot Box #k?\r\n#L0#What is a \"Overwatch Loot Box\"?#l\r\n#L1#Item list#l\r\n\r\n#L2#Open my #r#z4011021##l");
		} else if (status == 1) {
			if (selection == 0) {
				cm.sendOk("What is a #r#z4011021##k?\r\nThe Overwatch loot box can bought from the #bDonationPoints#k NPC! The loot box contains #rOverwatch#k related items.");
				cm.dispose();
              } else if (selection == 1) {
                    cm.sendOk("   ------#rItems that can be unlocked through this loot box!#k------\r\n\r\n#z1008545#\r\n#z1008546#\r\n#z1008551#\r\n#z1011007#\r\n#z1902015#\r\n#z1103019#\r\n\r\n#bNew items coming soon!#k");
                    cm.dispose();
			} else if (selection == 2) {
			if (!cm.haveItem(4011021, 1)) {
				cm.sendOk("You do not have any #r#z4011021##k");
				cm.dispose();
			} else {
				cm.sendYesNo("You have a #r#z4011021##k in your inventory!\r\nWould you like to open it?\r\n\r\n(#rNote#k: You must have 1 empty slot in Equip.)");
			}
			}
            // If we eventually wanna upgrade it into 3 categories depending on rareity.
		/*} else if (status == 2) {
			cm.gainItem(4011021, -1);
			if (chance > 0 && chance <= 2) {
				cm.gainItem(icommon, 1);
			} else if (chance >= 3 && chance <= 4) {
				cm.gainItem(inormal, 1);
			} else {
				cm.gainItem(irare, 1);
			}
			cm.dispose();
		}*/
		} else if (status == 2) {
			cm.gainItem(4011021, -1);
			if (chance > 0 && chance <= 2) {
				cm.gainItem(icommon, 1);
			} else if (chance >= 3 && chance <= 4) {
				cm.gainItem(icommon, 1);
			} else {
				cm.gainItem(icommon, 1);
			}
			cm.sendOk("SPARKLING EFFECT!!! Congratz! You recieved an item.");
			cm.dispose();
		}
	}
}
