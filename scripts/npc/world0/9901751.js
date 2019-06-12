load("scripts/util_party.js");
/* izarooni */
const destination = 97;
const membersMimum = 1;
let status = 0;

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
            if (partyExists(cm) && partySize(cm) >= membersMimum) {
                if (cm.isLeader()) {
                    let members = membersPresent(cm, player.getMapId());
                    if (members.present.length == partySize(cm)) {
                        for (let i = 0; i < members.present.length; i++) {
                            let mid = members.present[i];
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
            cm.sendNext("-------------------------#rThe Black Mage#k--------------------------------\r\n                                       Expedition\r\nRequirement:\r\n2 party members or more\r\nLevel 180+\r\n#r#z4011022##k#k");
        }
    } else if (status == 3) {

    }
}
