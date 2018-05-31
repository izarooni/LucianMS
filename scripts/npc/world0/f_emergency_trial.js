const GAEventManager = Java.type("com.lucianms.features.auto.GAutoEventManager");
const ETrial         = Java.type("com.lucianms.features.auto.AEmergencyTrial");
const ExpTable       = Java.type("constants.ExpTable");

/* izarooni */
let status = 0;
let currentEvent = GAEventManager.getCurrentEvent();
let returnMap;

function action(mode, type, selection) {
    if (currentEvent == null) {
        cm.dispose();
        return;
    }
    if (player.getMapId() == ETrial.EndMapID) {
        LeaveEvent(mode, type, selection);
    } else {
        EnterEvent(mode, type, selection);
    }
}

function LeaveEvent(mode, type, selection) {
    if (status == 0) {
        returnMap = currentEvent.popLocation(player.getId());
        if (returnMap != null) {
            cm.sendNext("Excellent work! Thanks for slaying that monster, he's been real trouble for me.", 1)
        } else {
            cm.sendOk("Looks like you have nowhere to return to");
            cm.dispose();
        }
    } else if (status == 1) {
        player.gainExp(Math.floor(ExpTable.getExpNeededForLevel(player.getLevel()) * 0.35), true, false);
        player.changeMap(returnMap);
        cm.dispose();
    }
}

function EnterEvent(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        cm.sendSimple("What's this? I can feel a dark presence beyond this portal. Should I see what is on the other side?\r\n#b"
            + "\r\n#L0#Enter the portal#l"
            + "\r\n#L1#I'll think about it#l", 2);
    }  else if (status == 2) {
        if (selection == 0) {
            currentEvent.registerPlayer(player);
        }
        cm.dispose();
    }
}
