var status = 0;

function start() {
	status = -1;
	action(1, 0, 0);
}

function action(mode, type, selection) {
	if (mode == -1) {
		cm.dispose();
	} else {
		if (mode == 0 && status == 0) {
			cm.dispose();
			return;
		}
		if (mode == 1)
			status++;
		else
			status--;
		if (status == 0) {
			cm.sendSimple("Are you interested in any of my offers?\r\n#b#L1# 1B mesos for 1 Crystal#l\r\n\#L2# 500M mesos for 1 orb#l");
		} else if (status == 1) {
			if (selection == 1) {
			if (cm.getMeso() >= 100000000) {
                cm.gainMeso(-100000000);
                cm.gainItem(4260002, 1);
				cm.sendOk("Congrats! You received a crystal!");
                cm.dispose();
            } else {
                cm.sendOk("Sorry, but you have to offer me more than that!");
                cm.dispose();
            }
			} else if (selection == 2) {
			if (cm.getMeso() >= 50000000) {
                cm.gainMeso(-50000000);
                cm.gainItem(4011022, 1);
				cm.sendOk("Congrats!");
                cm.dispose();
            } else {
                cm.sendOk("Sorry, but you have to offer me more than that!");
                cm.dispose();
            }
			}
		}
	}
}