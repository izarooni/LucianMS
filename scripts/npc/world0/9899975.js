/* izarooni
 JQ
 Warp NPC
*/

let status = 0;
let maps = [
    // [6, "#bTetris #k"],
    [105040310, "#gSave Gaberiel#k The Deep Forest of Patience <Step 1>"],
    [105040311, "#gSave Mickey#k The Deep Forest of Patience <Step 2>"],
    [105040312, "#gSave Mitch#k The Deep Forest of Patience <Step 3>"],
    [105040313, "#gsave Blaine#k The Deep Forest of Patience <Step 4>"],
    [105040314, "#gSave Brook#k The Deep Forest of Patience <Step 5>"],
    [105040315, "#gsave Susan#k The Deep Forest of Patience <Step 6>"],
    [105040316, "#gSave Denis#k The Deep Forest of Patience <Step 7>"],
    [922020000, "#gSave Amelia#k The Forgotten Darkness"],
    [610020000, "#gSave Clementine#k Valley of Heroes"],
    [682000200, "#bSave Ghost#k Ghost Chimney"],
    [280020000, "#bSave Savanah#k Breath of Lava 1"],
    [280020001, "#bSave Rose#k Breath of Lava 2"],
    [980044000, "#bSave Debby#k With Towerst floor"],
    [100000202, "#rSave Abby#k Pet-Walking Road"],
    [220000006, "#rSave Peter#k Ludibrium Pet Walkway"],
    [922240000, "#rSave Gaga#k Space Gaga"]

];

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        let content = "Hello, #h #!\r\nIt seems like all my friends are stuck and need a hand! \tCould you help them out?\r\n\r\n#eEvery JQ completion rewards Monster coins! Depending on the JQ difficulty, you will be rewarded different coins.\r\n\r\n\t\t\t\t\t#gGreen Monster coin#k - Hard\r\n\t\t\t\t\t#bBlue Monster coin#k - Medium\r\n\t\t\t\t\t#rRed Monster coin#k - Easy";
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
