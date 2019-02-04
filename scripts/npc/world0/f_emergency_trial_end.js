const GAEventManager = Java.type("com.lucianms.features.auto.GAutoEventManager");
const ETrial         = Java.type("com.lucianms.features.auto.AEmergencyTrial");
const ExpTable = Java.type("com.lucianms.constants.ExpTable");
/* izarooni */
let status = 0;
let currentEvent = GAEventManager.getCurrentEvent();

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        returnMap = currentEvent.popLocation(player.getId());
        if (returnMap != null) {
            cm.sendNext("Excellent work! Thanks for slaying that monster, he's been real trouble for me.", 1)
        } else {
            cm.sendOk("Looks like you have nowhere to return to");
            cm.dispose();
        }
    } else if (status == 2) {
        currentEvent.unregisterPlayer(player);
        player.gainExp(currentEvent.getExpGain(player.getLevel()), true, false);
        player.changeMap(returnMap);
        cm.dispose();
    }
}
