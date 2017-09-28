load("nashorn:mozilla_compat.js");
importPackage(Packages.server.events.custom.bpq);
var b = 3994115;
/* izarooni */
var status = 0;
var modes = [
    ["easy difficulty", BEasyMode],
    ["normal difficulty", BNormalMode],
    ["hard difficulty", BHardMode],
    ["hell difficulty", BHellMode]
];

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        var text = "You think you're strong enough to battle in the Boss PQs?\r\n Let me know which difficulty you are planning to tackle\r\n\r\n";
        for (var i = 0; i < 4; i++) {
            text += "\t#L" + i + "##v" + (b + i) + "##l";
        }
        cm.sendSimple(text);
    } else if (status == 2) {
        this.mode = modes[selection];
        cm.sendSimple("I want to battle in the " + this.mode[0] + "...#b"
            + "\r\n#L0#Alone#l"
            + "\r\n#L1#With my party#l", 2);
    } else if (status == 3) {
        if (selection == 0) {
            var pq = this.mode[1];
            pq = new pq(client.getChannel());
            pq.registerPlayer(player);
            pq.begin();
        } else if (selection == 1) {
            if (cm.getParty() != null) {
                if (cm.isLeader()) {
                    var pq = this.mode[1];
                    pq = new pq(client.getChannel());
                    var iter = cm.getParty().getMembers().iterator();
                    while (iter.hasNext()) {
                        var n = iter.next();
                        if (player.getMap().getCharacterById(n.getId()) != null) {
                            pq.registerPlayer(n.getPlayer());
                        }
                    }
                } else {
                    cm.sendOk("Only the party leader may decide when to enter the PQ");
                }
            } else {
                cm.sendOk("Find or create a party if you're looking for a group battle");
            }
        }
        cm.dispose();
    }
}