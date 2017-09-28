var LifeFactory = Java.type("server.life.MapleLifeFactory");
var MPC = Java.type("tools.MaplePacketCreator");
importPackage(Packages.tools);

function start() {
    cm.sendYesNo("Hi there.");
}

function action(mode, type, selection) {
    if (mode > 0) {
        cm.warp(122000003);
        cm.getPlayer().getMap().broadcastMessage(MaplePacketCreator.showEffect("quest/party/clear1"));
    }
    cm.dispose();
} 