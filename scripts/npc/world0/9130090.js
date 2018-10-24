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
        if (expedition == null) {
            cm.sendAcceptDecline("No time for chatter. Fight me.");
        } else {
        }
    } else if (status == 2) {

    }
}