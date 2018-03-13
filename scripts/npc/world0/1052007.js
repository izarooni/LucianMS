/* izarooni */
var status = 0;
var hasTicket = false;

function action(mode, type, selection) {
    if (mode < 1) {
        cm.dispose();
        return;
    } else {
        status++;
    }
    if (status == 1) {
        cm.sendSimple("Pick your destination.\r\n#b"
                    + "\r\n#L0#Kerning City Subway#l"
                    + "\r\n#L1#Kerning Square Shopping Center#l"
                    + "\r\n#L2#Enter Construction Site"
                    + "\r\n#L3#New Leaf City#l");
    } else if (status == 2) {
        if (selection == 0) {
            cm.warp(103000101);
            cm.dispose();
        } else if (selection == 1) {
            let name = "KerningTrain_" + player.getName();
            let em = cm.getEventManager("KerningTrain");
            em.removeInstance(name);
            em.newInstance(name);
            em.setProperty("player", player.getName());
            em.startInstance(player);
            cm.dispose();
        } else if (selection == 2) {
            if (cm.haveItem(4031036) || cm.haveItem(4031037) || cm.haveItem(4031038)) {
                let content = "You will be brought in immediately. Which ticket would you like to use?#b";
                for (let i = 0; i < 3; i++) {
                    if (cm.haveItem(4031036 + i)) content += "\r\n#L" + (i + 1) + "##t" + (4031036 + i) + "#";
                }
                cm.sendSimple(content);
                hasTicket = true;
            } else {
                cm.sendOk("It seems as though you don't have a ticket!");
                cm.dispose();
            }
        } else if (selection == 3) {
            if (player.getMapId() == 103000100 && cm.haveItem(4031711)) {
                let em = cm.getEventManager("Subway");
                if (em.getProperty("entry") == "true") {
                    cm.sendYesNo("It looks like there's plenty of room for this ride. Please have your ticket ready so I can let you in. The ride will be long, but you'll get to your destination just fine. What do you think? Do you wants to get on this ride?");
                } else {
                    cm.sendNext("We will begin boarding 1 minute before the takeoff. Please be patient and wait for a few minutes. Be aware that the subway will take off right on time, and we stop receiving tickets 1 minute before that, so please make sure to be here on time.");
                    cm.dispose();
                }
            } else {
                cm.sendOk("It seems you don't have a ticketr! You can buy one from Bell.");
                cm.dispose();
            }
        }
    } else if (status == 3) {
        if (hasTicket) {
            cm.gainItem(4031035 + selection, -1);
            cm.warp(103000897 +  (selection * 3));
            cm.dispose();
        }
        if (cm.haveItem(4031711)) {
            cm.gainItem(4031711, -1);
            cm.warp(600010004);
            cm.dispose();
        }
    }
}
