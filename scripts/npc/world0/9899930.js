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
        let content = "Welcome to #bThe Final World#k!"
            + "\r\nUmmm..You quickly saw me before but I do not think I have introduced myself to you. I am Chirithy\r\n"
            + "\r\nYou are now in #bThe Final World#k.";
        cm.sendNext(content);
    } else if (status == 2) {
        let content = "Now..I will repeat myself because I do not think you listened to me!\r\nYou are currently at the #bFinal World#k\r\n\r\nThis is a place where there's nothing beyond. When both body and heart perish together, you end up here..but do not worry. I will try to help you get back onto your feet!\r\nBut first..We must figure out to stop this world from splitting into two and creating parellels in the real world.";
        cm.sendNextPrev(content);
    } else if (status == 3) {
        let content = "Lets make it short..Currently the world is in great danger by a man with the name #rXehanort#k. He is spreading darkness and anxiety into other worlds and we must stop him!";
        cm.sendNextPrev(content);
    } else if (status == 4) {
        cm.sendNextPrev("Are you ready to continue into this beautiful world? Be careful! There are dangerous and strong creatures ahead!");
    } else if (status == 5) {
        cm.warp(90000001);
        player.announce(Packages.tools.MaplePacketCreator.showEffect("quest/party/clear3"));
        cm.dispose();
    }
}
