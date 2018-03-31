const MapleInventoryType = Java.type("client.inventory.MapleInventoryType");
/* izarooni */
let status = 0;
let inventory = player.getInventory(MapleInventoryType.EQUIP);

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        let content = "What weapon would you like to use for a Regalia?\r\n";
        let selections = "";
        inventory.list().forEach(function(item) {
            if (item.getItemId() >= 1302000 && item.getItemId() <= 1492044) {
                selections += "\t#L" + item.getPosition() + "##v" + item.getItemId() + "##l";
            }
        });
        if (selections.length > 0) {
            cm.sendSimple(content + selections);
        } else {
            cm.sendOk("You have no weapons available for creating a Regalia.");
            cm.dispose();
        }
    } else if (status == 2) {
        cm.sendOk(inventory.getItem(selection));
        cm.dispose();
    }
}
