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
        let content = "So to refresh your memory a bit, here's the leaders of the  #rDark Orginazation#k.\r\nThey might look a bit like each other but that's because it is the same person.\r\nNo one know's the name of the leaders for sure but it is rumored to be #rXehanort#k and that he is using #bTime Traveling#k to mess with the #eWorld#k";
        cm.sendNext(content);
    } else if (status == 2) {
        let content = "As I said before you, unfortunately lost the battle to stop him from messing with the world. I am sure with the right amount of training and focus, you'll be able to win the next battle!\r\nJust believe in yourself!";
        cm.sendNextPrev(content);
    } else if (status == 3) {
        let content = "Let' start focusing on your training so you can get your memories back and begin to prepare for the next battle.";
        cm.warp(90000002);
        player.announce(Packages.tools.MaplePacketCreator.showEffect("quest/party/clear3"));
        cm.dispose();
}
}
