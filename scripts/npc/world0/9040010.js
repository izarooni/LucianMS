load('scripts/util_gpq.js');
/* izarooni */
let status = 0;

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    let eim = player.getEventInstance();
    if (eim == null) {
        if (status == 1) return cm.sendNext("How did you get here?");
        else cm.warp(nFieldEnding);
    } else if (eim.isLeader(player)) {
        if (cm.haveItem(nItemRubian)) {
            cm.removeAll(nItemRubian);
            cm.showEffect("quest/party/clear");
            cm.playSound("Party1/Clear");
            let elapsed = Date.now() - eim.vars.ergothStart;
            let points = 1000 - Math.floor(elapsed / (100 * 60));
            cm.getGuild().gainGP(Math.max(100, points));
            eim.dispose();
        } else {
            cm.sendOk("This is your final challenge. Defeat the evil lurking within the Rubian and return it to me. That is all.");
        }
    } else {
        cm.sendOk("I will only speak to the party leader.");
    }
    cm.dispose();
}