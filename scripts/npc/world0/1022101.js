/* izarooni
 Agatha - Orbis Platform Usher
 Warp NPC
*/

let status = 0;
let maps = [
    // [6, "#bTetris #k"],
    [11, "#bWario#k"],
    // [810, "#gCasino#k"],
    [271000000, "#dFuture Gate#k"],
    [273000000, "#rTwilight Perion#k"],
    [211060010, "#bLion King Castle"],
    [219000000, "#dCokeTown#k"],
    [951000000, "#rMonster Park#k"],
	[808, "#eSandbox#k"]
];

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        let content = "Hello, #r#h ##k! I am the #bMap Warper#k, where would you like to go?\r\n";
        for (let i = 0; i < maps.length; i++) {
            let map_name = maps[i][1];
            content += `\r\n#L${i}#${map_name}#l`;
        }
        cm.sendSimple(content);
    } else if (status == 2) {
        if (selection >= 0 && selection < maps.length) {
            let map_id = maps[selection][0];
            if (map_id == 808) {// sandbox
                cm.openNpc(9899958);
                return;
            } else {
                cm.warp(maps[selection][0]);
             }
        }
        cm.dispose();
    }
}
