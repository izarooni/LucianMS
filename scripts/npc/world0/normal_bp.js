const Equip = Java.type('com.lucianms.client.inventory.Equip');
const MapleStat = Java.type('com.lucianms.client.MapleStat');
const ModifyInventory = Java.type('com.lucianms.client.inventory.ModifyInventory');
const InventoryType = Java.type('com.lucianms.client.inventory.MapleInventoryType');
const MaplePacketCreator = Java.type('tools.MaplePacketCreator');
const MapleCharacter = Java.type('com.lucianms.client.MapleCharacter');
const MapleInventoryManipulator = Java.type('com.lucianms.server.MapleInventoryManipulator');

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }

    if (status == 0) {
        cm.sendSimple("Hello. I'm the Chirithy #bBoss Points#k MSI NPC.\r\n"
            + "\r\n#L1#I would like to upgrade to a tier 2 boss item#l"
            + "\r\n#L2#I would like to upgrade to a tier 3 boss item#l"
            + "\r\n#L3#I would like to upgrade to a tier 4 boss item#l");
    }

    else if (status == 1) {
        
        if (selection == 0) {
            tier = 2;
            let dialog = "Certainly. A tier 1 MSI requires the following:\r\n 25 Normal Boss Points#k#n#k\r\n\r\n#nWould you like to upgrade to a tier 2 MSI?";
            cm.sendYesNo(dialog);
        }
        else if (selection == 1) {
            tier = 3;
            let dialog = "Certainly. A tier 1 MSI requires the following:\r\n 25 Hard Boss Points#k#n#k\r\n\r\n#nWould you like to upgrade to a tier 3 MSI?";        
            cm.sendYesNo(dialog);
        }
        else if (selection == 2) {
            tier = 4;
            let dialog = "Certainly. A tier 1 MSI requires the following:\r\n 25 Hell Boss Points#k#n#k\r\n\r\n#nWould you like to upgrade to a tier 4 MSI?";
            cm.sendYesNo(dialog);
        }
    }
    else if (status == 2) {


    }

}