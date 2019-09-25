/* 

*/
var status = 0;

function action(mode, type, selection) {
	if (mode < 1) return cm.dispose();
	else status++;

	if (status == 1) {
		cm.sendSimple("#eAbsent Silouhette#k - Difficulity #r***#k\r\n\r\nBeyond this #eAbsent Silouhette,#k exists an powerful enemy. Do you wish to enter?!\r\n#r#L1#Let's go#k#l\r\n\#b#L2#I'm not ready yet#k#l");
	} else if (status == 2) {
		if (selection == 1) {
			cm.warp(551030901, 0);
            cm.player.dropMessage("Defeat the Mysterious Adversary!");
			cm.dispose();
		} else if (selection == 2) {
			cm.sendOk("Come back when you are ready.");
			cm.dispose();
		}
	}
}