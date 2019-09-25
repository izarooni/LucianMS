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
        cm.sendSimple("What would you like to do? If you have never participated in the Monster Carnival, you'll need to know a thing or two about it before joining.\r\n#b#L0# Go to the Monster Carnival Field.#l");
    } else if (status == 2) {
        player.saveLocation("MIRROR");
        cm.warp(980000000);
        cm.dispose();
    }
}
