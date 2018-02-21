/* izarooni
 Agatha - Orbis Platform Usher
 Warp NPC
*/

var status = 0;
var maps = [
    [6, "#bTetris #k"],
    [11, "#bWario#k"],
    [810, "#gCasino#k"],
    [271000000, "#dFuture Gate#k"],
    [273000000, "#rTwilight Perion#k"],
    [610050000, "#bBlackGate City#k"],
    [211060010, "#bLion King Castle"],
    [219000000, "#dCokeTown#k"],
    [951000000, "#rMonster Park#k"],
    [910000025, "#bSmash The Wall#k"],
    [910001000, "#dHarvesting Area#k"]
];

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        var content = "Hello, #r#h ##k! I am the #bMap Warper#k, where would you like to go?\r\n";
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
