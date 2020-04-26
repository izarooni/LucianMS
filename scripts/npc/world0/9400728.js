/* izarooni */
let status = 0;

function action(mode, type, selection) {
    if (mode == -1) {
        cm.dispose();
        return;
    } else if (mode == 0) {
        status--;
    } else {
        status++;
    }
    if (status == 1) {
        cm.sendNext("So..as you see, I have made some holograms for you.\r\nThis is #rXehanort#k in his younger and older version.\r\nHe is using time traveling abilities to go by his younger version in this world..\r\n\r\nAnd by doing that he cheats the time system by using his older self in the future.");
    } else if (status == 2) {
        cm.sendNextPrev("He is very dangerous! We must stop him. Lets go, quick!");
    } else if (status == 3) {
        // let content = "Let' start focusing on your training so you can get your memories back and begin to prepare for the next battle.";
        //player.announce(Packages.tools.MaplePacketCreator.showEffect("quest/party/clear3"));
        cm.warp(90000007);
        cm.dispose();
}
}
