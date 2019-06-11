const ServerConstants = Java.type('com.lucianms.constants.ServerConstants');
const InventoryType = Java.type('com.lucianms.client.inventory.MapleInventoryType');
const ItemConstants = Java.type('com.lucianms.constants.ItemConstants');
const InventoryModifier = Java.type('com.lucianms.server.MapleInventoryManipulator');
/* izarooni */
const DIVORCE_COST = 8;
let status = 0;

let inventory = InventoryType.EQUIP;
let ring = findRing(player, inventory);
if (!ring.isPresent()) {
    ring = findRing(player, (inventory = InventoryType.EQUIPPED));
}

function findRing(target, inventoryType) {
    return target.getInventory(inventoryType).list().stream()
        .filter(i => ItemConstants.isWeddingRing(i.getItemId()))
        .findFirst();
}

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    let rel = player.getRelationship();
    let formal = rel.getStatus();
    if (formal != formal.class.static.Married) {
        cm.sendOk("You must be married before you can get divorced.");
        cm.dispose();
        return;
    }

    let partnerName = undefined;
    if (rel.getGroomId() == player.getId()) partnerName = rel.getBrideUsername();
    else if (rel.getBrideId() == player.getId()) partnerName = rel.getGroomUsername();
    if (partnerName == undefined) {
        cm.sendOk("You are not married");
        return cm.dispose();
    }
    let partner = client.getWorldServer().findPlayer(p => p.getName().equalsIgnoreCase(partnerName));


    if (status == 1) {
        cm.sendNext("Unlucky. You want out of your relationship? It'll cost you, though."
            + `\r\nA divorce will cost #b8 #z${ServerConstants.CURRENCY}##k. Are you able to pay that?`);
    } else if (status == 2) {
        if (cm.haveItem(ServerConstants.CURRENCY, DIVORCE_COST)) {
            cm.sendYesNo(`Are you sure you want to divorce from #b${partnerName}#k?`);
        } else {
            cm.sendOk("No. You don't have that much.");
            cm.dispose();
        }
    } else if (status == 3) {
        cm.gainItem(ServerConstants.CURRENCY, -DIVORCE_COST, true);
        if (ring.isPresent()) {
            let r = ring.get();
            InventoryModifier.removeFromSlot(client, inventory, r.getPosition(), 1, false);
        }
        rel.reset();
        if (partner != null) {
            ring = findRing(partner, (inventory = InventoryType.EQUIP));
            if (!ring.isPresent()) {
                ring = findRing(partner, (inventory = InventoryType.EQUIPPED));
                if (ring.isPresent()) {
                    InventoryModifier.removeFromSlot(partner.getClient(), inventory, ring.get().getPosition(), 1, false);
                }
            }
            partner.getRelationship().reset();
        } else {
            // the ring has been removed from 1 party and because the other party is offline,
            // the server will realize a divorce has been settled and remove the ring
            player.sendNote(partnerName, player.getName() + " has divorced you.", 1);
        }

        cm.sendOk(`You are now divorced from ${partnerName}`)
        cm.dispose();
    }
}