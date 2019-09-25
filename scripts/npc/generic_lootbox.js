load('scripts/util_imports.js');
var lootbox = undefined;

/* izarooni */
let status = 0;

function action(mode, type, selection) {
    if (mode < 1) {
        return cm.dispose();
    }
    status++;
    let keys = Object.keys(lootbox);

    if (status == 1) {
        cm.vars = {};
        if (keys.length > 1) {
            let content = "Which are you interested in?\r\n\r\n";
            for (let i = 0; i < keys.length; i++) {
                content += `#L${i}##i${keys[i]}##l\t`;
                if (i > 0 && i % 3 == 0) content += "\r\n";
            }
            cm.sendSimple(content);
        } else {
            cm.vars.lootbox = keys[0];
            cm.sendSimple(`Do you have any #b#z${cm.vars.lootbox}#?#k I will gladly take them off your hands in exchange for some cool items\r\n#b`
                + "\r\n#L0#Tell me more#l"
                + "\r\n#L1#What can I get from opening it?#l"
                + `\r\n#L2#Open my #t${cm.vars.lootbox}#`);
        }
    } else if (status == 2) {
        if (keys.length > 1) {
            for (let i = 0; i < keys.length; i++) {
                if (i != selection) delete lootbox[keys[i]];
            }
            status = 0;
            return action(1, 0, 0);
        }
        if (selection == 0) return DlgAbout(lootbox[cm.vars.lootbox].about);
        else if (selection == 1) return DlgRewards(lootbox[cm.vars.lootbox].items);
        else if (selection == 2) {
            if (player.isDebug()) cm.gainItem(cm.vars.lootbox, 1, true);
            if (cm.haveItem(cm.vars.lootbox)) {
                return cm.sendNext(`Are you sure you want to give me your #b#t${cm.vars.lootbox}##k?`);
            } else {
                cm.sendOk(`You do not have any #b#t${cm.vars.lootbox}#`);
            }
        }
        cm.dispose();
    } else if (status == 3) {
        if (cm.haveItem(cm.vars.lootbox)) {
            let items = lootbox[cm.vars.lootbox].items;
            if (items == undefined || items.length == 0) {
                cm.sendOk(`There are currently no rewards for #b#z${cm.vars.lootbox}#.`);
                return cm.dispose();
            }
            let item = items[Math.floor(Math.random() * items.length)];
            if (InventoryModifier.checkSpace(client, item, 1, "")) {
                cm.gainItem(cm.vars.lootbox, -1, true);
                cm.gainItem(item, 1, true);
            }
        } else {
            cm.sendOk(`You do not have any #b#t${cm.vars.lootbox}#`);
        }
        cm.dispose();
    }
}
function DlgAbout(content) {
    status = 0;
    if (content == undefined)
        cm.sendNext("This feature is incomplete. Come back another time!");
    else
        cm.sendNext(content);
}

function DlgRewards(items) {
    status = 0;
    if (items == undefined || items.length == 0) {
        return cm.sendNext(`There are currently no rewards for #b#z${cm.vars.lootbox}#.`);
    }
    let content = `Here is a list of things you can possibly receive\r\n#b`;
    for (let i = 0; i < items.length; i++) {
        content += `\r\n#v${items[i]}#\t#z${items[i]}#`;
    }
    cm.sendNext(content);
}