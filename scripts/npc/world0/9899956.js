/*
 * @Author:     Venem
 * Map:     Plaza final NPC
*/
var status = 0;

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == -1) {
        cm.dispose();
    } else {
        if (mode == 0 && status == 0) {
            cm.dispose();
            return;
        }
        if (mode == 1)
            status++;
        else
            status--;
        if (status == 0) {
            cm.sendSimple("So you've finally arrived. I was expecting you.\r\n\r\nI assume you're here because you want to escape my #rPlaza#k? Is that correct? Well..Too bad that you'd have to test my strength first.\r\n\r\n                                     Do you accept?\r\n#r#L1#Yes! I will fight you and I will defeat you!#k#l\r\n\#b#L2#I need more practice.#k#l");
        } else if (status == 1) {
            if (selection == 1) {
                cm.warp(551030393, 0);
                cm.dispose();
            } else if (selection == 2) {
                cm.sendOk("I'll be waiting...");
                cm.dispose();
            }
        }
    }
}