/* izarooni */
var status = 0;

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        let content = "Welcome to #rLucianMS#k"
            + "\r\nTo play this server, you will need our custom WZ files. Because you're talking to me, we can assume you already have them and are ready to go!"
            + "\r\nIf you find a bug, crash or any other issues with gameplay, please submit it on our forums!"
            + "\r\n#dAre you ready to continue in the tutorial?";
        cm.sendNext(content);
    } else if (status == 2) {
        cm.warp(90000001);
        player.announce(Packages.tools.MaplePacketCreator.showEffect("PSO2/stuff/1"));
        cm.dispose();
    }
}