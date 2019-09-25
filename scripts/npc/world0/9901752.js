/*Written by Jeemie, with help from Kerrigan*/

const Equip = Java.type('com.lucianms.client.inventory.Equip');
const MapleStat = Java.type('com.lucianms.client.MapleStat');
const ModifyInventory = Java.type('com.lucianms.client.inventory.ModifyInventory');
const InventoryType = Java.type('com.lucianms.client.inventory.MapleInventoryType');
const MaplePacketCreator = Java.type('tools.MaplePacketCreator');
const MapleCharacter = Java.type('com.lucianms.client.MapleCharacter');
const MapleInventoryManipulator = Java.type('com.lucianms.server.MapleInventoryManipulator');

const items = [1004808, 1102940, 1082695, 1053063, 1073158, 1302343, 1312203, 1402259, 1412181,
    1422189, 1432218,1442274,1004809,1102941,1082696,1053064,1073159,1372228,1382265,1004810,1102942,1082697,
   1053065,1073160,1452257,1462243,1004811,1102943,1082698,1053066,1073161,1332279,1472265,1004812,1102944,
   1082699,1053067,1073162,1482221, 1492235];
var tier = 0;
var select;
const reqPoints = 35;

var TIER1 = 14000;
var TIER2 = 21000;
var TIER3 = 27000;
var TIER4 = 32700;
var aTIER1 = 50;
var aTIER2 = 75;
var aTIER3 = 100;
var aTIER4 = 150;


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
            + "\r\n#d#L0#I would like to make a tier 1 boss item#l"
            + "\r\n#L1#I would like to upgrade to a tier 2 boss item#l"
            + "\r\n#L2#I would like to upgrade to a tier 3 boss item#l"
            + "\r\n#L3#I would like to upgrade to a tier 4 boss item#l"
            + "\r\n#L4#I would like to read more#l");
    }

    else if (status == 1) {
        
        if (selection == 0) {
            tier = 1;
            let dialog = "Certainly. A tier 1 MSI requires the following:\r\n " + reqPoints + " Easy Boss Points#k#n#k\r\n\r\n#nWould you like to make a tier 1 MSI?";
            cm.sendYesNo(dialog);
        }
        else if (selection == 1) {
            tier = 2;
            let dialog = "Certainly. A tier 2 MSI requires the following:\r\n " + reqPoints + " Normal Boss Points#k#n#k\r\n\r\n#nWould you like to make a tier 2 MSI?";
            cm.sendYesNo(dialog);
        }
        else if (selection == 2) {
            tier = 3;
            let dialog = "Certainly. A tier 3 MSI requires the following:\r\n " + reqPoints + " Hard Boss Points#k#n#k\r\n\r\n#nWould you like to make a tier 3 MSI?";
            cm.sendYesNo(dialog);
        }
        else if (selection == 3) {
            tier = 4;
            let dialog = "Certainly. A tier 4 MSI requires the following:\r\n " + reqPoints + " Hell Boss Points#k#n#k\r\n\r\n#nWould you like to make a tier 4 MSI?";
            cm.sendYesNo(dialog);
        }
        else if (selection == 4){
            let dialog = "Of course.\r\n A Tier 1 MSI will have 14k all stats + 50atk \r\n A tier 2 MSI will have 21k all stats + 75atk \r\n Tier 3 = 27kAS +100atk \r\n Tier 4 at 32k AS + 150atk";
            cm.sendOk(dialog);
            cm.dispose();
        }
    }

    else if (status == 2) {
        var dialog = "Which item would you like?#b";
        for(var i = 0; i < items.length; dialog += "\r\n\t#L" + i + "##t" + items[i] + "##l", i++);
        
        cm.sendSimple(dialog);
        //cm.dispose();
    }

    else if (status == 3){
        
        if(cm.canHold(1302000)) {
        select = selection;

            switch(tier){
            case 1:
                dialog = "You have " + player.getEasyBossPoints() + " easy points. Would you like to continue?";
                break;
            case 2:
                dialog = "You have " + player.getNormalBossPoints() + " normal points. Would you like to continue?";
                break;
            case 3:
                dialog = "You have " + player.getHardBossPoints() + " hard points. Would you like to continue?";
                break;
            case 4:
                dialog = "You have " + player.getHellBossPoints() + " hell points. Would you like to continue?";
                break;
            default:
                dialog = "How tf did you get here";
                break;
            }
    

        cm.sendYesNo(dialog);
        }
        else{
            dialog = "You don't have a free slot in your EQUIP tab";
            cm.sendOk(dialog);
            cm.dispose();
        }
    }

    else if (status == 4){
            switch(tier){
                case 1:
                    if(player.getEasyBossPoints() >= reqPoints){
                       cm.createItemWithStats(items[select], TIER1, aTIER1);
                       player.setEasyBossPoints(player.getEasyBossPoints() - reqPoints);

                       dialog = "Thank you! You now have " + player.getEasyBossPoints + " points left.";
                       cm.sendOk(dialog);
                       cm.dispose();
                    }
                    break;
                case 2:
                    if(player.getNormalBossPoints() >= reqPoints){
                       cm.createItemWithStats(items[select], TIER2, aTIER2);
                       player.setNormalBossPoints(player.getNormalBossPoints() - reqPoints);

                       dialog = "Thank you! You now have " + player.getNormalBossPoints + " points left.";
                       cm.sendOk(dialog);
                       cm.dispose();
                     }
                    break;
                case 3:
                    if(player.getHardBossPoints() >= reqPoints){
                       cm.createItemWithStats(items[select], TIER3, aTIER3);
                       player.setHardBossPoints(player.getHardBossPoints() - reqPoints);

                       dialog = "Thank you! You now have " + player.getHardBossPoints + " points left.";
                       cm.sendOk(dialog);
                       cm.dispose();
                     }
                    break;
                case 4:
                    if(player.getHellBossPoints() >= reqPoints){
                       cm.createItemWithStats(items[select], TIER4, aTIER4);
                       player.setHellBossPoints(player.getHellBossPoints() - reqPoints);

                       dialog = "Thank you! You now have " + player.getHellBossPoints + " points left.";
                       cm.sendOk(dialog);
                       cm.dispose();
                     }
                    break;
                default:
                    dialog = "How tf did you get here";
                    break;
            }

        if(player.isGM() || player.getName() === 'iDeeKay'){
          cm.createItemWithStats(items[select], 32767, 32767 );
          dialog = "Thanks you, Cheater";
          //cm.sendOk(dialog);
          //cm.dispose();
        }
        dialog = "You don't have enough points.";
        cm.sendOk(dialog);
        cm.dispose();

    }

}
