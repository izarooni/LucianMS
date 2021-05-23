//FeinT 2020

var item1 = 4000363;
var amount = 1;
var status = 0;

function start() {
	status = -1;
	action(1, 0, 0);
}

function action(mode, type, selection) {
	if (mode == -1) {
		cm.dispose();
	} else {
		if (mode == 0) {
			cm.sendOk("Im..Hurt...");
			cm.dispose();
			return;
		}
		  status++;
		if (status == 0) {
			cm.sendNext("Hey Stranger, Can you help me please?");
		} else if (status == 1) {
			if (cm.haveItem(item1, amount)) {
				if(cm.getMeso() >= 500) {
					cm.warp(330000003);
					cm.removeAll(4000363);
					cm.gainMeso(-500);

					cm.sendOk("Thank you kind stranger. As my appreciation I have given you my bus ticket for half its price. Have a good day!");
					cm.dispose();
				}
				else{
					cm.sendOk("I'll give you my ticket for half price at 500 mesos as my appreciation... but you don't have it..");
					cm.dispose();
				}
			} else {
                                cm.sendOk("A wild maple leaf came out of nowhere and messed me up pretty bad, can you find it and show it whos boss?\r\nIt also took my bus ticket.. So if you find it, Can you bring it back here?\r\n\r\nI saw it run straight into the city on the right side. Please hurry! Take this to help!");
								cm.createItemWithStatsAndUpgradeSlots(1002562,10,10,6);
                                cm.dispose();
		       }
			  cm.dispose();
		}
	}
}