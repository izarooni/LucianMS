load('scripts/util_imports.js');
load('scripts/util_transaction.js');
const GoldenLeaf = 4000313;
/* izarooni 
Cameow
Golden Leaves Trader
*/
let status = 0;
let selections = [
    [50, [1002761, 1]],
    [50, [1302062, 1]],
    [150, [1003027, 1]],
    [600, [1003068, 1]],
    [999, [1082399, 1]],
];
let playerChoice;

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        let content = `Hello, I am Cameow! The cutest kitty around. I have some sweet prizes in exchange for some #b#z${GoldenLeaf}##k.\r\nAre you interested in any of these prizes?\r\n#b`;
        for (let i = 0; i < selections.length; i++) {
            let cost = selections[i][0];
            let offer = selections[i][1];
            content += `\r\n#L${i}#${cost} #v${GoldenLeaf}# for ${offer[1]} #z${offer[0]}##l`;
        }
        cm.sendSimple(content);
    } else if (status == 2) {
        playerChoice = selections[selection];
        let cost = playerChoice[0];
        let offer = playerChoice[1];
        cm.sendNext(`Are you sure you want to trade ${cost} #b#z${GoldenLeaf}##k for ${offer[1]} #b#z${offer[0]}##k?`);
    } else if (status == 3) {
        let cost = playerChoice[0];
        let offer = playerChoice[1];
        if (cm.hasItem(GoldenLeaf, cost)) {
            if (InventoryModifier.checkSpace(client, offer[0], offer[1], "")) {
                cm.gainItem(GoldenLeaf, -cost, true);
                cm.gainItem(offer[0], offer[1], true);

                let log = `${player.getName()} traded ${cost} ${GoldenLeaf} for ${offer[1]} of ${offer[0]}`;
                let transactionId = createTransaction(cm.getDatabaseConnection(), player.getId(), log);
                if (transactionId == -1) {
                    print("Error creating transaction log...");
                    print(log);
                }
                cm.sendOk("Enjoy!\r\n#kHere is your transaction ID: #b" + transactionId);
                cm.dispose();
            } else {
                cm.sendOk("Please make room in your inventory.");
            }
        } else {
            cm.sendOk(`You do not have ${cost} #z${GoldenLeaf}#`);
        }
        cm.dispose();
    }
}