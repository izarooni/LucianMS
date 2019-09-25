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
        cm.sendSimple("      I always dreamed of being the strongest..You see, I once also were a hero but I was manipulated by a guy with yellow eyes and my hatred began to take control of me. You must take this mask and find out the truth between light and the dark. \r\n#L0##i1012299##bTake Mask#k");
    } else if (status == 2) {
        if (selection == 0) {
            cm.gainItem(1012299, 1);
            cm.warp(86, 0);
        }
        cm.dispose();
    }
}
