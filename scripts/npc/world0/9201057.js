const KerningSubway = 103000100;
const NLCSubway = 600010001;

let isStation = player.getMapId() == KerningSubway || player.getMapId() == NLCSubway;
let isKerning = player.getMapId() == KerningSubway;
let destinationName = isKerning ? "New Life City of Masteria" : "Kerning City of  vicotria Island";
let ticket = (4031711 + parseInt(player.getMapId() / 300000000));

/* izarooni */
let status = 0;

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        if (isStation) {
            cm.sendYesNo(
                "The ride to " + destinationName + " takes will depart soon, and it'll cost you #b5000 mesos#k. "
                + "Are you sure you want to purchase #b#t" + ticket + "##k?");
        } else {
            cm.sendYesNo("Do you want to leave before the train start? There will be no refund.");
        }
    } else if (status == 2) {
        if (isStation) {
            if (cm.getMeso() >= 5000) {
                cm.gainMeso(-5000);
                cm.gainItem(ticket, 1, true);
            } else {
                cm.sendNext("You don't have enough mesos.");
            }
        } else {
            cm.warp(player.getMapId() == KerningSubway + 1 ? KerningSubway : NLCSubway);
        }
        cm.dispose();
    }
}
