load("scripts/util_imports.js");
var MaplePacketCreator = Java.type("tools.MaplePacketCreator");
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
        cm.sendGetNumber("Would you like to try my game of Roulette?\r\nAll you have to do is match these 3 rocks -- (placeholder text)\r\n", 1000, 1000, player.getMeso());
    } else if (status == 2) {
        if (selection < 1000) {
            status = 0;
            cm.sendNext("You must put at least #b1,000 mesos#k");
        } else if (selection > player.getMeso()) {
            status = 0;
            cm.sendNext("Insufficient funds");
        } else {
            cm.sendNext("Are you sure you want to bet #b" + StringUtil.formatNumber(selection) + " mesos#k?");
        }
    } else if (status == 3) {
        // concenpt
        player.announce(MaplePacketCreator.showEffect("miro/frame"));
        player.announce(MaplePacketCreator.showEffect("miro/RR1/" + Math.floor(Math.random() * 4) ));
        player.announce(MaplePacketCreator.showEffect("miro/RR2/" + Math.floor(Math.random() * 4) ));
        player.announce(MaplePacketCreator.showEffect("miro/RR3/" + Math.floor(Math.random() * 5) ));

        // var achieve = player.getAchievement("Win both casino games");
        // achieve.setCasino2Completed(true);
        // Java.type("scripting.Achievements").testFor(player, -1);

        cm.delayCall(function() {
            cm.sendOk("Did you win? Did you lose?\r\nWell I don't fuckin' know! I need more information before I can be finished");
        }, 5700); // very educated guess
        cm.dispose();
    }
}