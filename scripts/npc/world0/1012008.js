const ServerConstants = Java.type('com.lucianms.constants.ServerConstants');
const OmokGames = [4080000, 4080001, 4080002, 4080003, 4080004, 4080005, 4080006, 4080007, 4080008, 4080009, 4080010, 4080011];
const MatchCards = 4080100;
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
        let content = `Buying a game set will cost #b10 #z${ServerConstants.CURRENCY}##k. What would you like to buy?#b`;
        content += `\r\n#L0##z${MatchCards}##l\r\n`;
        for (let i = 1; i < OmokGames.length; i++) {
            content += `\r\n#L${i}##z${OmokGames[i]}##l`;
        }
        cm.sendSimple(content);
    } else if (status == 2) {
        let selectedItem = selection == 0 ? MatchCards : OmokGames[selection - 1];
        cm.vars = { item_id: selectedItem };
        if (cm.haveItem(ServerConstants.CURRENCY, 10)) {
            cm.sendNext(`Are you sure you want to buy\r\n#b#z${selectedItem}##k for 10 #z${ServerConstants.CURRENCY}#?`);
        } else {
            cm.sendOk(`Make sure you have #b10 #z${ServerConstants.CURRENCY}##k before trying to make a trade with me.`);
            cm.dispose();
        }
    } else if (status == 3) {
        if (cm.canHold(cm.vars.item_id)) {
            cm.gainItem(ServerConstants.CURRENCY, -10);
            cm.gainItem(cm.vars.item_id, 1, true);
            cm.sendOk("Thank you! I hope you enjoy your game~");
        } else {
            cm.sendOk("Make sure you have room in your ETC inventory.");
        }
        cm.dispose();
    }
}
