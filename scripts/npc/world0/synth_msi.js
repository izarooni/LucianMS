const Equip         = Java.type('com.lucianms.client.inventory.Equip');
const InventoryType = Java.type('com.lucianms.client.inventory.MapleInventoryType');

const Results = [
    new MSI(1302000, [[4000000, 1], [2043003, 1]], { "str": 32767, "dex": 32767 }),
    new MSI(1372161, [[4000000, 1], [2043702, 1]], { "int": 32767, "matk": 32767 })
];

/* izarooni */
let status = 0;

function action(mode, type, selection) {
    if (cm.vars == null) cm.vars = {};
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        if (Results.length == 0) {
            cm.sendOk("There are no items available to synthesize right now.");
            cm.dispose();
            return;
        }
        let content = "What item would you like to make?\r\n#b";
        for (let i = 0; i < Results.length; i++) {
            let msi = Results[i];
            content += `\r\n#L${i}##v${msi.itemID}# - #z${msi.itemID}##l`;
        }
        cm.sendSimple(content);
    } else if (status == 2) {
        let msi = (cm.vars.msi = Results[selection]);
        let content = "You will need the following items to synthesize this MSI\r\n";
        let reqs = msi.requirements;
        for (let i = 0; i < reqs.length; i++) {
            let item = reqs[i];
            if (!cm.haveItem(item[0], item[1])) {
                msi.canCreate = false;
            }
            let pHaveQuantity = player.getItemQuantity(item[0], true);
            content += `\r\n${item[1]}/${pHaveQuantity} of #v${item[0]}#`;
        }
        cm.sendNext(content);
    } else if (status == 3) {
        let msi = cm.vars.msi;
        if (msi.canCreate) {
            if (player.getInventory(MapleInventoryType.EQUIP).isFull()) {
                cm.sendOk("Please make room in your EQUIP inventory first!");
                cm.dispose();
                return;
            }
            // remove requirements from player inventory
            let reqs = msi.requirements;
            for (let i = 0; i < reqs.length; i++) {
                cm.gainItem(reqs[i][0], -reqs[i][1]);
            }

            // create MSI
            let eq = new Equip(msi.itemID, 0);
            for (let stat in msi.modStats) {
                if (msi.modStats.hasOwnProperty(stat)) {
                    eq.setStat(stat, msi.modStats[stat]);
                }
            }
            cm.gainItem(eq, true);
        } else {
            cm.sendNext("You do not have the necessary items to synthesize this MSI");
        }
        cm.dispose();
    }
}

function MSI(itemID, requirements, modStats) {
    this.itemID = itemID;
    this.requirements = requirements;
    this.modStats = modStats;
    this.canCreate = true;
}