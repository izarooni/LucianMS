const Arcade = Java.type('com.lucianms.features.arcade.Arcade');
const Minigame = Java.type("com.lucianms.features.arcade.BoxSpider");
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
        cm.sendSimple("Are you daring enough to test your luck in this Box Spider minigame?\r\n#b"
            + `\r\n#L1#Play Box Spider (Current Highscore: ${Arcade.getHighscore(2, cm.getPlayer())})#l`
            + "\r\n#L2#How to play#l"
            + "\r\n#L3#Rankings#l"); 
    } else if (status == 2) {
        if (selection == 1) {
            let minigame = new Minigame();
            minigame.registerPlayer(player);
            minigame.start();
        } else if (selection == 2) {
            cm.sendOk("When playing this game there is 2 main objectives \r\n\r\n#r1. Do not get touched by the spiders coming down \r\n2. Destroy the boxes to get points#k\r\n\r\nIf your highscore is in the top 50, you'll be listed in the highscores.");
        } else if (selection == 3) {
            var fuck = Arcade.getTop(2);
            cm.sendOk("This is the current top 50 of Box Spider \r\n\r\n" + (fuck == null ? "#rThere are no highscores yet..#" : fuck));
        }
        cm.dispose();
    }
}