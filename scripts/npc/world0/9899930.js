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
        let content = "Welcome to #rMapleChirithy#k!"
            + "\r\nHi! I am Chirithy!\r\nTo play this server, you will need our custom WZ files. Because you're talking to me, we can assume you already have them and are ready to go!\r\n"
            + "\r\nIf you find a bug, crash or any other issues with gameplay, please submit it on our forum or discord (which ever may be available to you at the time).";
        cm.sendNext(content);
    } else if (status == 2) {
        let content = "Now..You might wonder where you are.\r\nYou are currently at the #bFinal World#k\r\n\r\nThis is a place where there's nothing beyond. When both body and heart perish together, you end up here..but do not worry. I will try to help you get back onto your feet!\r\nNow, how did you end up here? Well...long story...";
        cm.sendNextPrev(content);
    } else if (status == 3) {
        let content = "Lets make it short..Currently the world is in great danger by a man with the name #rXehanort#k. He is spreading darkness and anxiety into other worlds and we must stop him!";
        cm.sendNextPrev(content);
    } else if (status == 4) {
        let content = "OH!! One more thing!\r\n\r\n#FUI/StatusBar/BtNPT/normal/0# - The Trade Button\r\n"
            + "\r\nNext to the cash shop button in the bottom right of your screen you'll have the Trade button to access NPCs such as #bcurrency traders#k, #bplayer styler#k and the #bjob advancer#k."
            + "\r\n\r\nThis button will definitely not be enabled everywhere but if it is disabled in a map where you think otherwise, please let us know!";
        cm.sendNextPrev(content);
    } else if (status == 5) {
        cm.sendNextPrev("Are you ready to continue in the tutorial? There is no turning back once you continue.");
    } else if (status == 6) {
        cm.warp(90000001);
        player.announce(Packages.tools.MaplePacketCreator.showEffect("quest/party/clear3"));
        cm.dispose();
    }
}
