/* izarooni */
const EXIT_MAP = 103000890;
const TOWN_MAP = 103000000;
const BONUS_MAP = 103000805;
let status = 0;

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (player.getMapId() == EXIT_MAP) {
        if (status == 1) {
            cm.sendNext("See you next time.");
        } else if (status == 2) {
            var map = client.getChannelServer().getMap(TOWN_MAP);
            cm.removeAll(4001007);
            cm.removeAll(4001008);
            player.changeMap(map, map.getRandomSpawnpoint());
            cm.dispose();
        }
    } else if (status == 1) {
        if (player.getMapId() == BONUS_MAP) {
            cm.sendYesNo("Are you ready to leave this map");
        } else {
            cm.sendYesNo("Once you leave the map, you'll have to restart the whole quest if you want to try it again.  Do you still want to leave this map?");
        }
    } else if (status == 2) {
        var eim = player.getEventInstance();
        if (eim == null) {
            player.changeMap(EXIT_MAP);
        } else if (cm.isLeader()) {
            eim.disbandParty();
        } else {
            eim.leftParty(player);
        }
        cm.dispose();
    }
}
