const MaplePacketCreator = Java.type('tools.MaplePacketCreator');

const AbandonedSubwayFieldID = 910320000;
const BattlefieldFieldID = 910320001; 
const RestingSpotFieldID = 910320001;
/* izarooni */
let status = 0;
let func = undefined;

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (player.getMapId() == AbandonedSubwayFieldID) {
        return EntranceHandle(selection);
    } else if (player.getMapId() == RestingSpotFieldID) {
        let eim = player.getEventInstance();
        if (eim != null) eim.dispose();
        else cm.warp(AbandonedSubwayFieldID);
        return cm.dispose();
    } else {
        return ExitHandle(selection);
    }
}

function ExitHandle(selection) {
    if (status == 1) {
        cm.sendNext("Are you ready to leave?");
    } else {
        let eim = player.getEventInstance();
        if (eim != null) {
            eim.removePlayer(player);
        } else {
            player.changeMap(AbandonedSubwayFieldID);
        }
        cm.dispose();
    }
}

function EntranceHandle(selection) {
    if (func != undefined) return func(selection);
    if (status == 1) {
        cm.sendSimple("Hi, I'm Mr. Lim\r\n#b"
            + "\r\n#e#L0#Head down the Dusty Platform.#l#n"
            + "\r\n#L1#Head to Train 999.#l");
    } else if (status == 2) {
        if (selection == 0) func = TransferDustyPlatform;
         else if (selection == 1) {
             cm.warp(Train999FieldID);
             return cm.dispose();
         }
        status = 0;
        action(1, 0, 0);
    }
}

function TransferDustyPlatform(selection) {
    let em = cm.getEventManager("DustyPlatform");
    if (em == null) {
        cm.sendOk("Seems the Dusty Platform is under construction right now. Check again later");
        return cm.dispose();
    }
    if (status == 1) {
        cm.sendSimple("Would you like to go now?\r\n#b"
        + "\r\n#L0#Go alone.#l"
        + "\r\n#L1#Go with a party of 2 or more.#l");
    } else if (status == 2) {
        if (selection == 0) {
            em.startInstance(player);
        } else {

        }
    }
}