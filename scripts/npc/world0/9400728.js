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
        cm.sendNext("So..as you see, I have made some holograms.\r\nThis is #rXehanort#k in his younger and older version.\r\nHe is using time traveling abilities to go by his younger version which is very scary..\r\n\r\nYou have been lost for a very, very long time since he stroke you down in the last battle but I have faith that someday you will avenge yourself.");
    } else if (status == 2) {
        cm.sendNextPrev("Now! Let us continue so we can get you back onto earth, alright? See you further in!!");
    } else if (status == 3) {
        // let content = "Let' start focusing on your training so you can get your memories back and begin to prepare for the next battle.";
        //player.announce(Packages.tools.MaplePacketCreator.showEffect("quest/party/clear3"));
        cm.warp(90000002);
        cm.dispose();
}
}
