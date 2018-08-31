/* izarooni
 Agatha - Orbis Platform Usher
 Warp NPC
*/

var status = 0;
var maps = [
    [807300100, "Mori Ranmau - Tier 1"],
    [262030300, "Hilla - Tier 2"],
    [105200710, "Crimson Queens - Tier 2"],
    [105200110, "Von Bon - Tier 3"],
    [105200610, "Pierre - Tier 3 "],
	[940100007, "Magnus - Tier 4"],
    [86, "#rRealm of Gods#k - Tier 4+ #rWarning:#k Boss Tier"]
];

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        var content = "Hello, #b#h ##k! would you like to fight some #rBosses#k?, pick one.\r\n\r\nWhat do I receive from defeating bosses?\r\n#eEach boss drops a different #bBoss Tier Coin#k. These coins can be used in exchange of powerful equipment!#k";
        for (var i = 0; i < maps.length; i++) {
            content += "\r\n#L" + i + "#" + maps[i][1] + "#l";
        }
        cm.sendSimple(content);
    } else if (status == 2) {
        if (selection >= 0 && selection < maps.length) {
            cm.warp(maps[selection][0]);
        }
        cm.dispose();
    }
}
