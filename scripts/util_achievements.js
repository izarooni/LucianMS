const InventoryModifier = Java.type('com.lucianms.server.MapleInventoryManipulator');
const MaplePacketCreator = Java.type('tools.MaplePacketCreator');
const ServerConstants = Java.type('com.lucianms.constants.ServerConstants');
const MapleInventoryType = Java.type('com.lucianms.client.inventory.MapleInventoryType');

function tryGiveItem(player, arr) {
    for (let i = 0; i < arr.length; i++) {
        let ri = arr[i];
        if (ri.type == undefined) {
            if (!InventoryModifier.checkSpace(player.getClient(), ri.itemID, ri.quantity, "")) {
                return false;
            }
        } else if (ri.type == "meso") {
            if (player.getMeso() + ri.quantity > 2147483647) {
                return false;
            }
        }
    }
    for (let i = 0; i < arr.length; i++) {
        let ri = arr[i];
        if (ri.type == undefined) {
            InventoryModifier.addById(player.getClient(), ri.itemID, ri.quantity);
            player.announce(MaplePacketCreator.getShowItemGain(ri.itemID, ri.quantity, true));
        } else {
            player.gainMeso(ri.quantity, true);
        }
    }
    return true;
}

function RewardItem(itemID, quantity, type) {
    this.itemID = itemID;
    this.quantity = quantity;
    this.type = type;
}