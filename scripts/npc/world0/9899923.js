/*
 * @author: FeinT
 */
var MaplePacketCreator = Java.type("tools.MaplePacketCreator");
var status = 0;
function start() {
    if (cm.canHold(4011024)) {
        cm.warp(978);
        cm.gainItem(4011024, 7);
        player.dropMessage("You have successfully completed The Breath of Lava (Stage 1)! You have received 7 arcade coins.");
    } else {
        cm.sendOk("Please make room in your ETC inventory before talking to me.");
    }
    cm.dispose();
}
