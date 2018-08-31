/* izarooni
 Agatha - Orbis Platform Usher
 Warp NPC
*/

var status = 0;
var maps = [
    [4, "#bWhispy Woods PVP#k"],
    [93, "#rYear 2228 City PVP#k"]
];

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        var content = "Hello, #r#h ##k! I am the #rPVP Master#k, where would you like to go?\r\n";
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
