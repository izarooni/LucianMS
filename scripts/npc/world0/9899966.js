const Arcade = Java.type('com.lucianms.features.arcade.Arcade');
const Minigame = Java.type("com.lucianms.features.arcade.BalrogKiller");
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
        cm.sendSimple("Are you daring enough to test your luck in this Balrog Killer minigame?\r\n#b"
            + `\r\n#L1#Play Balrog Killer (Current Highscore: ${Arcade.getHighscore(4, cm.getPlayer())})#l`
            + "\r\n#L2#How to play#l"
            + "\r\n#L3#Rankings#l");
    } else if (status == 2) {
        if (selection == 1) {
            let minigame = new Minigame();
            minigame.registerPlayer(player);
            minigame.start();
        } else if (selection == 2) {
            cm.sendOk("When playing this game there is 2 main objectives \r\n\r\n#r1. Do not get killed by the balrog \r\n2. Kill the balrog to get points#k\r\n\r\nIf your highscore is in the top 50, you'll be listed in the highscores.");
        } else if (selection == 3) {
            var fuck = Arcade.getTop(2);
            cm.sendOk("This is the current top 50 of Balrog Killer \r\n\r\n" + (fuck == null ? "#rThere are no highscores yet..#" : fuck));
        }
        cm.dispose();
    }
}