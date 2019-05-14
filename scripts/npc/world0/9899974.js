/* izarooni
 Agatha - Orbis Platform Usher
 Warp NPC
*/

let status = 0;
let maps = [
    // [6, "#bTetris #k"],
    [11, "#bWario#k"],
    [810, "#gCasino#k"],
    [86, "#rRealm of Gods#k"],
    [749050500, "#bFishing Map 1#k"],
    [749050501, "#bFishing Map 2#k"],
    [749050502, "#bFishing Map 3#k"],
    [96, "#rClass Gates#k"],
    [808, "#eSandbox#k"],
    [324, "#bFinal World (NPCS)#k"],
    [271000000, "#dFuture Gate#k"],
    [273000000, "#rTwilight Perion#k"],
    [211060010, "#bLion King Castle"],
    [219000000, "#dCokeTown#k"],
    [551030800, "#bNoragami Aragato#k"],
    [951000000, "#rMonster Park#k"],
    [099999999, "#gSuper Mario Bros 1-1#k"],
    [925100500, "#bBoss Spawner#k"],
    [599000006, "#rUltimate Mushroom#k"]
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