/* izarooni
 Agatha - Orbis Platform Usher
 Warp NPC
*/

let status = 0;
let maps = [
    [910000000, "Stop the bus at The Free Market"],
    [100000000, "Stop the bus at Henesys"]
];

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        let content = "The bus is about to stop. Which bus stop do you desire to stop at?\r\n";
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
				cm.sendOk("Welcome to #bChirithy#k.\r\n\r\nLet me tell you about some important stuff to help you out on your new journey.\r\n\r\n#FUI/StatusBar/BtNPT/normal/0# - The Trade Button - Next to the cash shop button in the bottom right of your screen you'll have the Trade button to access NPCs such as certain #bcurrency traders#k, #bplayer styler#k and the #bjob advancer#k\r\n\r\nFor a display of player commands simply type #e@help#n and all commands will display in the chat box.\r\n\r\nIf you have any other questions you can contact any online staff member by typing #e@callGM#n in the chat box or perhaps ask an online player.\r\n\r\n#eHave fun!#n");

					cm.dispose();
             }
        }
        cm.dispose();
    }
}
