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
        cm.sendNext("I've been waiting for you for a decade.");
    } else if (status == 2) {
        cm.sendNext("You who posses big and great strengt and power. Power so powerfull that it's unstoppeable.");
    } else if (status == 3) {
        cm.sendNext("The shadow realm has opened and the evil monsters has returned to the future world. My time is up so this will be on you.");
    } else if (status == 4) {
        cm.sendNext("You will leave your current body 5 years behind and take over your future body.");
    } else if (status == 5) {
        cm.sendNext("Here..Take this #r Millennium #k Puzzle with you. This way you'll always have me by your side no matter what. I'll be within the puzzle all the time.");
    } else if (status == 6) {
        cm.sendNext("There are other heroes out there who already are taking their part in the fight versus the evil monsters! But none like you. You're special.");
    } else if (status == 6) {
        cm.sendOK("Now go! I'll be with you inside the puzzle.");
        cm.gainItem(1302000, 1);
        cm.gainItem(1302001, 1);
        cm.warp(122000000);
        cm.getPlayer().getMap().broadcastMessage(MaplePacketCreator.showEffect("quest/party/clear5"));
        cm.dispose();
    }
}