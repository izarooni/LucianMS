/*
 * @author: FeinT
 */
var MaplePacketCreator = Java.type("tools.MaplePacketCreator");
var status = 0;
function start() {
    if (cm.canHold(4011024)) {
        cm.warp(978);
        cm.gainItem(4011024, 5);
        player.dropMessage("You have successfully completed The Deep Forest of Patience (Stage 2)! You have received 5 arcade coins.");
    } else {
        cm.sendOk("Please make room in your ETC inventory before talking to me.");
    }
    cm.dispose();
}
