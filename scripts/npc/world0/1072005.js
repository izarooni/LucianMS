function start() {
    if (cm.haveItem(4031013,30)) {
        cm.sendNext("Ohhhhh.. you collected all 30 Dark Marbles!! It should have been difficult.. just incredible! Alright. You've passed the test and for that, I'll reward you #bThe Proof of a Hero#k. Take that and go back to Ellinia.");
    } else {
        cm.sendSimple("You will have to collect me #b30 #t4031013##k. Good luck. \r\n#b#L1#I would like to leave#l");
    }
}

function action(mode, type, selection) {
    if (mode == 1) {
		if(selection == 1){
			cm.warp(101020000, 0);
		}else{
			cm.warp(101020000, 0);
			cm.removeAll(4031013);
			cm.gainItem(4031009, -1);
			cm.gainItem(4031012);
		}
	}
	cm.dispose();
}