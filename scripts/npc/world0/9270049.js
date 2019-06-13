const GiveItem = 1012272;
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
        cm.sendSimple("#rWait#k...\r\n\r\nI sense that I was being controlled. But by who?\r\n"
            + "Take this mask, it has incredible power and I have a feeling it might help you in the future...\r\n"
            + `#L0##i${GiveItem}# #bTake the mask#k`);
    } else if (status == 2) {
        cm.gainItem(GiveItem, 1, true);     //Mask of the Ghoul
        cm.warp(86);
    }
}
