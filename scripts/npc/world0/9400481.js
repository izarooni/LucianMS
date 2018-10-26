load("nashorn:mozilla_compat.js"); // to load all boss pq classes
importPackage(Packages.com.lucianms.features.bpq);
const b = 3994115; // difficulty represented as an item
/* izarooni */
let selectedMode = { pq: null, mode: null };
let status = 0;
let modes = [
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
        let text = "You think you're strong enough to battle against several bosses back to back?\r\n Let me know which difficulty you are planning to tackle\r\n\r\n";
        for (var i = 0; i < 4; i++) {
            text += "\t#L" + i + "##v" + (b + i) + "##l";
        }
        cm.sendSimple(text);
    } else if (status == 2) {
        selectedMode.mode = modes[selection][0];
        let pq = modes[selection][1];
        selectedMode.pq = new pq(client.getChannel());
        let minLevel = selectedMode.pq.getMinimumLevel();

        if (player.getLevel() >= minLevel) {
            cm.sendNext("The minimum level recommendation for this mode is #b" + minLevel + "#k\r\nIf don't believe you are strong enough, don't try.");
        } else {
            cm.sendOk("You must be at least level #b" + minLevel + "#k to do this mode.");
            cm.dispose();
        }
    } else if (status == 3) {
        cm.sendSimple("I want to battle in the " + selectedMode.mode + "...#b"
            + "\r\n#L0#Alone#l"
            + "\r\n#L1#With my party#l", 2);
    } else if (status == 4) {
        if (selection == 0) {
            selectedMode.pq.registerPlayer(player);
            selectedMode.pq.begin();
        } else if (selection == 1) {
            if (cm.getParty() != null) {
                if (cm.isLeader()) {
                    let iter = cm.getPartyMembers().iterator();
                    let map = player.getMap();
                    while (iter.hasNext()) {
                        let n = iter.next();
                        if (map.getCharacterById(n.getId()) != null) {
                            selectedMode.pq.registerPlayer(n);
                        }
                    }
                    selectedMode.pq.begin();
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
