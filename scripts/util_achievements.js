const InventoryModifier = Java.type('com.lucianms.server.MapleInventoryManipulator');
const MaplePacketCreator = Java.type('tools.MaplePacketCreator');
const ServerConstants = Java.type('com.lucianms.constants.ServerConstants');
const MapleInventoryType = Java.type('com.lucianms.client.inventory.MapleInventoryType');

function tryGiveItem(player, arr) {
    for (let i = 0; i < arr.length; i++) {
        let ri = arr[i];
        if (!InventoryModifier.checkSpace(player.getClient(), ri.itemID, ri.quantity, "")) {
            return false;
        }
    }
    for (let i = 0; i < arr.length; i++) {
        let ri = arr[i];
        InventoryModifier.addById(player.getClient(), ri.itemID, ri.quantity);
        player.announce(MaplePacketCreator.getShowItemGain(ri.itemID, ri.quantity, true));
    }
    return true;
}

function RewardItem(itemID, quantity) {
    this.itemID = itemID;
    this.quantity = quantity;
}