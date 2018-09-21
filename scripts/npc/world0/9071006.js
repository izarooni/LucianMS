/* izarooni */
// npc: 9071000
var status = 0;

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        cm.sendOk("Are you ready for Monster Park? Alone or with a party, when you're ready just enter through a gate over there!");
        cm.dispose();
    }
}
