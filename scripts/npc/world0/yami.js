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
        cm.sendNext("Text 1.");
    } else if (status == 2) {
        cm.sendNext("Text 2.");
    } else if (status == 3) {
        cm.sendNext("Text 3.");
    } else if (status == 4) {
        cm.sendNext("Text 4.");
    } else if (status == 5) {
        cm.sendNext("Text 5.");
    } else if (status == 6) {
        cm.sendNext("Text 6.");
    } else if (status == 6) {
        cm.sendNext("Text 7.");
        cm.gainItem(1302000, 1);
        cm.gainItem(1302001, 1);
        cm.warp(122000000);
        cm.dispose();
    }
}