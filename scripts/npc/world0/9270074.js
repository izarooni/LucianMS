//script by Alcandon

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
			cm.sendSimple("Hey #b#h ##k! I #eSinon#n understand that nostalgic is important to many so if you miss some #eLucianMS#n specific maps that were removed, why not speak to me?\r\nIf you have any other map in mind speak to an #rAdmin!#k\r\n#L0#Universal Galaxy FM\r\n#L1#Arks Quest Ship");
		} else if (selection == 0) {
			cm.warp(900000001);
			cm.dispose();
		} else if (selection == 1) {
			cm.warp(867118100);
			cm.dispose();
		}
	}
}

