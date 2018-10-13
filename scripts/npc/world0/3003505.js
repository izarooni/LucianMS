/* izarooni */
let status = 0;

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
		cm.sendSimple("You defeated me. I suppose you are worth enough to see the world for what it is. Take this mask and find out the truth between light and darkness. \r\n#L0##i1012299##bTake Mask#k");
    } else if (status == 2) {
		if (Selection == 0) {
			cm.gainItem(1012299, 1);
			cm.warp(86, 0);
		}
		cm.dispose();
	}
}
