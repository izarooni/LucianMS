function start() {
    cm.sendYesNo("\r\n\r\n\r\nWould you like to go to the #rPVP#k map?");
}

function action(mode, type, selection) {
    if (mode > 0) {
        cm.warp(4);
    }
    cm.dispose();
} 