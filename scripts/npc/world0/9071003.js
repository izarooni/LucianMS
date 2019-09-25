function start() {
    if(cm.getPlayer().getMapId() != 951000000) {
        cm.sendYesNo("Would you like to go to the Monster Park?");
    } else {
        cm.sendYesNo("Would you like to go back to the home map?");
    }
}

function action(mode, type, selection) {
    if (mode > 0) {
        if (cm.getPlayer().getMapId() != 951000000) {
            cm.warp(951000000);
        } else {
            cm.warp(910000000);
        }
    }
    cm.dispose();
} 