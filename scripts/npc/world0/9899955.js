load('scripts/util_imports.js');
const FieldBuilder = Java.type('com.lucianms.server.FieldBuilder');
const EntryLimits = Java.type("tools.EntryLimits");
const ENTRY_TYPE = "auto_rb";
const ONE_DAY = (1000 * 60 * 60 * 24);
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
        let entry = EntryLimits.getEntries(player.getId(), ENTRY_TYPE);
        if (entry != null) {
            if (entry.LastEntry + ONE_DAY  < Date.now() || player.isDebug()) {
                EntryLimits.reset(player.getId(), ENTRY_TYPE);
            } else {
                cm.sendOk("You can only enter the Datascape once per day."
                 + `\r\nCome back in #b${StringUtil.getTimeElapse((entry.LastEntry + ONE_DAY) - Date.now())}#k.`);
                return cm.dispose();
            }
        }
        cm.sendNext("Are you ready to enter the Datascape?"
            + `\r\nInside you will break boxes around the map and try to obtain #b#z${ServerConstants.getAutoRebirthItem()}##k`
            + "\r\n");
    } else if (status == 2) {
        EntryLimits.incrementEntry(player.getId(), ENTRY_TYPE);
        let map = new FieldBuilder(client.getWorld(), client.getChannel(), 89).loadAll().build();
        player.changeMap(map);
        cm.dispose();
    }
}