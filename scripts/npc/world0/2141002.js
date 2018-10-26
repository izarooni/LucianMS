const BossPQ = Java.type("com.lucianms.features.bpq.BossPQ");
/* izarooni */
let status = 0;
let pq = player.getGenericEvents().stream().filter(e => e instanceof BossPQ).findFirst().orElse(null);

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        cm.sendSimple("Do you want to leave the #rBossPQ#k?\r\n#b"
            + "\r\n#L0#Yes, I want to leave#l"
            + "\r\n#L1#No, I'll continue#l");
    } else if (status == 2) {
        if (selection == 0) {
            if (pq != null) {
                pq.unregisterPlayer(player);
            } else {
                cm.warp(809);
            }
        }
        cm.dispose();
    }
}