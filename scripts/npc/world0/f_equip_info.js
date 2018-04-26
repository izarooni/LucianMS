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
        let item = inventory.getItem(selection);
        this.selectedItem = item;
        let content = "#v" + item.getItemId() + "#";
        content += "\r\nWeapon Level: " + item.getItemLevel();
        content += "\r\nEliminations: " + item.getEliminations();
        content += "\r\nRegalia: " + item.isRegalia();
        content += "\r\n\r\n#bWould you like to make this item your Regalia?";
        cm.sendNext(content);
    } else if (status == 3) {
        this.selectedItem.setRegalia(!this.selectedItem.isRegalia());
        cm.sendOk("\t\t\t\t\t\t  #eYour #v" + this.selectedItem.getItemId() + "# is " + (this.selectedItem.isRegalia() ? "now" : "no longer") + " a Regalia");;
    }
}
