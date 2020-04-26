const ServerConstants = Java.type('com.lucianms.constants.ServerConstants');
const Items = [1022999, 1022995, 1022996];
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
        let content = "Here #h #. I am the dealer for #bEye Scanners#k!\r\n"
            + "Are you interested in any of these? Eye Scanners increases the drop rate of #rDragon Balls#k by #g10%#k\r\n#b";
        for (let i = 0; i < Items.length; i++) {
            content += `\r\n#L${i}# Trade 3\t #i${ServerConstants.CURRENCY}# \tfor\t #i${Items[i]}##l`;
        }
        cm.sendSimple(content);
    } else if (status == 2) {
        cm.vars = { item: Items[selection] };
        cm.sendNext(`Are you sure you want to buy the #b#z${cm.vars.item}##k for 6 #z${ServerConstants.CURRENCY}#?`);
    } else if (status == 3) {
        if (cm.haveItem(ServerConstants.CURRENCY, 3)) {
            if (cm.canHold(cm.vars.item)) {
                cm.gainItem(ServerConstants.CURRENCY, -3);
                cm.gainItem(cm.vars.item, 1, true);
                cm.sendOk("Pleasure doing business with you");
                cm.dispose();
            } else {
                cm.sendOk("Please make sure you have enough room in your equip inventory");
            }
        } else {
            cm.sendOk(`You do not have 3 #b#z${ServerConstants.CURRENCY}#`)
        }
        cm.dispose();
    }
}
