function start() {
    cm.sendYesNo("Would you like to go to the Monster Park?");
}

function action(mode, type, selection) {
    if (mode > 0) {
        cm.warp(951000000);
    }
    cm.dispose();
} 