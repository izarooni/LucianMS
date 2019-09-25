//By Venem
//Defeat Genos reward
function start() {
    cm.sendSimple("#rWait#k...\r\n\r\nI sense that I was being controlled. But by who?\r\nTake this mask, it has incredible power and I have a feeling it might help you in the future...\r\n#L0##i1012272##bTake Mask#k");
}

function action(mode, type, selection) {
    cm.dispose();
    if (selection == 0) {
        cm.gainItem(1012272, 1);     //Mask of the Ghoul
        cm.warp(86, 0);
        cm.dispose();
    }
}
