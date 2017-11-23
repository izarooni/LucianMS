var MaplePacketCreator = Java.type("tools.MaplePacketCreator");
var LifeFactory = Java.type("server.life.MapleLifeFactory");

/* izarooni */
var status = 0;

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        cm.sendOk("How's mommys little baby today? Your friends called. They want you to play with them at the park but if you're heading out, please bring some milk back on the way.?");
    } else if (status == 2) {
        cm.warp(32);
        cm.getPlayer().getMap().broadcastMessage(MaplePacketCreator.showEffect("quest/party/clear5"));
        cm.dispose();
    }
}
