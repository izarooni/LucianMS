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
        cm.sendNext("#eNeed some help?", 1);
    } else if (status == 2) {
        player.changeMap(90000012);
        player.sendMessage("You have received a blessing from the Light");
        cm.dispose();
    }
}