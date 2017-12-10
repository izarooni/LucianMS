importPackage(Packages.tools);
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
        cm.sendNext("My time is up in this world. I need to head back into the #r Tomb of the nameless pharaoh #k");
    } else if (status == 2) {
        cm.sendNext("I heard someone mention that my name was #r Atem, #k but I am not quite sure.");
    } else if (status == 3) {
        cm.sendNext("My puzzle seems to sense some kind of weird power within you.");
    } else if (status == 4) {
        cm.sendNext("I think time's up..but before I leave..");
    } else if (status == 5) {
        cm.sendNext("Take this. Something just tells me that my puzzle belongs with you.");
    } else if (status == 6) {
        cm.sendNext("Watch out though. The greater evil is after it and their leader is #r The Black Mage. r/n/ The Puzzle can sense when something is wrong and will help you out.");
    } else if (status == 6) {
        cm.sendNext("Goodbye friend.");
        cm.gainItem(1012089, 1);
        cm.warp(122000000);
        cm.dispose();
        cm.getPlayer().getMap().broadcastMessage(MaplePacketCreator.showEffect("quest/party/clear5"));
    }
}
