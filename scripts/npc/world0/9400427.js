load('scripts/util_imports.js');
load('scripts/util_transaction.js');
const HalloweenCandy = 4031203;
/* izarooni 
Cameow
*/
let status = 0;
let selections = [
    [999, [4260002, 1]],
    [600, [4011020, 1]],
    [400, [4011027, 1]],
    [200, [4011026, 1]]
    /*[2000, [4011020, 1]],
    [1000, [4011027, 1]],
    [500, [4011026, 1]],*/
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
        let content = `Hello, I am Cameow! The cutest kitty around. I have some sweet prizes in exchange for some #b#z${HalloweenCandy}##k.\r\nAre you interested in any of these prizes?\r\n#b`;
        for (let i = 0; i < selections.length; i++) {
            let cost = selections[i][0];
            let offer = selections[i][1];
            content += `\r\n#L${i}#${cost} #v${HalloweenCandy}# for ${offer[1]} #z${offer[0]}##l`;
        }
        cm.sendSimple(content);
    } else if (status == 2) {
        playerChoice = selections[selection];
        let cost = playerChoice[0];
        let offer = playerChoice[1];
        cm.sendNext(`Are you sure you want to trade ${cost} #b#z${HalloweenCandy}##k for ${offer[1]} #b#z${offer[0]}##k?`);
    } else if (status == 3) {
        let cost = playerChoice[0];
        let offer = playerChoice[1];
        if (cm.hasItem(HalloweenCandy, cost)) {
            if (InventoryModifier.checkSpace(client, offer[0], offer[1], "")) {
                cm.gainItem(HalloweenCandy, -cost, true);
                cm.gainItem(offer[0], offer[1], true);

                let log = `${player.getName()} traded ${cost} ${HalloweenCandy} for ${offer[1]} of ${offer[0]}`;
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
            cm.sendOk(`You do not have ${cost} #z${HalloweenCandy}#`);
        }
        cm.dispose();
    }
}