const MapleInventoryType = Java.type("com.lucianms.client.inventory.MapleInventoryType");
const MaplePacketCreator = Java.type("tools.MaplePacketCreator");
const SporeItem = 2430014;
/* izarooni */
var status = 0;
var qStatus = player.getQuestStatus(2323);

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        if (qStatus < 2) {
            let item = player.getInventory(MapleInventoryType.USE).findById(SporeItem);
            if (item != null) {
                cm.sendYesNo("#bDo you want to use the Kill Mushroom Spore?#k\r\n\r\n"
                    + "#r#e<Caution>#n\r\nNot for human consumption!\r\nIf ingested, seek medical attention immediately!", 2);
            } else {
                player.announce(MaplePacketCreator.earnTitleMessage("You cannot move forward due to the barrier."));
                cm.dispose();
            }
        } else {
            status++; // status 2 to increment to 3
            action(1, 0, 0);
        }
    } else if (status == 2) {
        cm.sendNext("Success! The barrier is broken!", 3);
    } else if (status == 3) {
        if (qStatus < 2) {
            cm.gainItem(SporeItem, -1);
            player.sendMessage(5, "The Mushroom Forest Barrier has been removed and penetrated.");
        }
        cm.warp(106020400);
        cm.dispose();
    }
}
