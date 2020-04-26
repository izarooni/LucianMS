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
        cm.sendNext("#eThe energy from this orb seems weird. Perhaps it is worth checking out", 1);
    } else if (status == 2) {
        player.changeMap(90000015);
		player.announce(Packages.tools.MaplePacketCreator.showEffect("quest/party/clear5"));
        player.sendMessage("You traveled through time! Current year: Unknown");
        cm.dispose();
    }
}