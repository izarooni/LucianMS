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
        let content = "Welcome to #rLucianMS#k!"
            + "\r\nTo play this server, you will need our custom WZ files. Because you're talking to me, we can assume you already have them and are ready to go!\r\n"
            + "\r\nIf you find a bug, crash or any other issues with gameplay, please submit it on our forum or discord (which ever may be available to you at the time).";
        cm.sendNext(content);
    } else if (status == 2) {
        let content = "#FUI/StatusBar/BtNPT/normal/0# - The Trade Button\r\n"
            + "\r\nNext to the cash shop button in the bottom right of your screen you'll have the Trade button to access NPCs such as #bcurrency traders#k, #bplayer styler#k and the #bjob advancer#k."
            + "\r\n\r\nThis button will definitely not be enabled everywhere but if it is disabled in a map where you think otherwise, please let us know!";
        cm.sendNextPrev(content);
    } else if (status == 3) {
        cm.sendNextPrev("Are you ready to continue in the tutorial?");
    } else if (status == 4) {
        cm.warp(90000001);
        player.announce(Packages.tools.MaplePacketCreator.showEffect("PSO2/stuff/1"));
        cm.dispose();
    }
}
