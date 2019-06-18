/*
 * @author: FeinT
 */
var MaplePacketCreator = Java.type("tools.MaplePacketCreator");
var status = 0;
function start() {
    if (cm.canHold(4011020)) {
        cm.warp(910000000, 5);
        cm.gainItem(4011020, 1);
        player.announce(MaplePacketCreator.showEffect("quest/party/clear"));
        player.dropMessage("Jump Quest completed! You received a Monster Coin");
    } else {
        cm.sendOk("Please make room in your ETC inventory before talking to me.");
    }
    cm.dispose();
}
