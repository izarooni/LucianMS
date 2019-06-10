const MaplePacketCreator = Java.type('tools.MaplePacketCreator');
/* venem */
/* Outer Space Mini Boss and 4 planets entrance */
let status = 0;

function action(mode, type, selection) {
    if (mode < 1) {
        return;
    } else {
        status++;
    }
    if (status == 1) {
        cm.sendSimple("A #rBlack Hole#k appeared and it seems to attract everything into it. Must be some trick from #rThe Black Mage#k and his time traveling ability. Willing to take the risk? \r\n#b#L1#I am ready.#k#l\r\n\#r#L2#I am not ready yet.#k#l");
    } else if (status == 2) {
        if (selection == 1) {
            cm.warp(41);
            player.annouce(MaplePacketCreator.showEffect("quest/party/clear2"));
        } else {
            cm.sendOk("Hurry up or we might miss this opportunity!");
        }
        cm.dispose();
    }
}
