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
    if (status == 1) {
        cm.sendYesNo("Would you like to exit the Guild Quest?");
    } else if (status == 2) {
        let eim = player.getEventInstance();
        if (eim != null) {
            eim.removePlayer(player);
        } else {
            player.changeMap(nFieldConstructionSite);
        }
        cm.dispose();
    }
}