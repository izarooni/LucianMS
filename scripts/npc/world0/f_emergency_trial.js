const GAEventManager = Java.type("com.lucianms.features.auto.GAutoEventManager");
const ETrial         = Java.type("com.lucianms.features.auto.AEmergencyTrial");
/* izarooni */
let status = 0;
let currentEvent = GAEventManager.getCurrentEvent();
let returnMap;

function action(mode, type, selection) {
    if (currentEvent == null) {
        cm.dispose();
        return;
    }
    if ([ETrial.BossMapID, ETrial.TransferMapID, ETrial.WaitingMapID].indexOf(player.getMapId()) == -1) {
        EnterEvent(mode, type, selection);
    } else {
        cm.sendOk("Hmmm... I wonder what Von Leon is up to.");
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
