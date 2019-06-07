/* izarooni
 Agatha - Orbis Platform Usher
 Warp NPC
*/

var status = 0;
var maps = [
    [807300100, "Mori Ranmau"],
    [262030300, "Hilla"],
    [105200710, "Crimson Queens"],
    [105200110, "Von Bon"],
    [105200610, "Pierre"],
	[940100007, "Magnus"],
    [86, "#rRealm of Gods#k"]
];

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        var content = "Hello, #b#h ##k! would you like to fight some #rBosses#k?, pick one.\r\n\r\nWhat do I receive from defeating bosses?\r\n#ePowerfull gear will drop from the bosses upon their defeat so stay on your toes!";
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
