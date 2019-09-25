/*
 * @Author:     Venem
 * Map:     Plaza battle won
*/
let status = 0;

function action(m, t, s) {
    if (m < 1) { 
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        cm.sendNext("Can you feel it? The #rMoons#k power! It consumes me!\r\nIf you would like a taste of this power, enter through my gate.");
    } else if (status == 2) {
        cm.warp(96);
        cm.dispose();
    }
}