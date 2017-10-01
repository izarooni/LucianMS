load("scripts/util_party.js");
/* izarooni */
var status = 0;
var destination = 10000;
var membersMimum = 2;

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        cm.sendSimple("You want to fight the Black Mage?\r\nYou will need a party of at least #b" + membersMimum + " members#k to enter.\r\n#b#L0#I want to enter#l\r\n#L1#Tell more more about the Black Mage#l");
    } else if (status == 2) {
        if (selection == 0) {
            if (partyExists() && partySize() >= membersMimum) {
                if (cm.isLeader()) {
                    var members = membersPresent(player.getMapId());
                    if (members.present.length == partySize()) {
                        for (var i = 0; i < members.present.length; i++) {
                            var mid = members.present[i];
                            player.getMap().getCharacterById(mid).changeMap(destination);
                        }
                    } else {
                        cm.sendOk("Make sure all members of the party are online and are in the same map as you before entering");
                    }
                } else {
                    cm.sendOk("Only your leader may decide when to enter");
                }
            } else {
                cm.sendOk("You will need a party of at least #b" + membersMimum + " members#k to enter this PQ");
            }
            cm.dispose();
        } else {
            status = 0;
            cm.sendNext("More information coming soon!");
        }
    } else if (status == 3) {

    }
}