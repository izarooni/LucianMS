/* 

*/
var MaplePacketCreator = Java.type("tools.MaplePacketCreator");
var status = 0;

function action(mode, type, selection) {
	if (mode < 1) return cm.dispose();
	else status++;

	if (status == 1) {
		cm.sendSimple("HURRAY!! Seems like you did it! Do you wanna head back out?\r\n#r#L1#Yes#k#l\r\n\#b#L2#No#k#l");
	} else if (status == 2) {
		if (selection == 1) {
            cm.warp(220080000);
            player.announce(MaplePacketCreator.showEffect("quest/party/clear"));
        player.dropMessage("You can now use the command @autorb for 8 hours!");
			cm.dispose();
		} else if (selection == 2) {
			cm.sendOk("Come back when you want to head back out.");
			cm.dispose();
		}
	}
}