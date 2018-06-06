//Monster Gear NPC Trader
//Venem

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == -1) {
        cm.dispose();
    } else {
        if (mode == 0 && status == 0) {
            cm.dispose();
            return;
        }
        if (mode == 1)
            status++;
        else
            status--;
        if (status == 0) {
            cm.sendSimple ("Hello, there. Do you love #gMonster Energy#k? Then I got the right deal for you! \r\n\Are you interested in any of the equipments? \r\n\ #L0# Trade 6 #i4011020# for 1 #i1442259# \r\n #L1# Trade 6 #i4011020# for 1 #i1008554# \r\n\ #L2# Trade 6 #i4011020# for 1 #i1082975#"); 
                  } else if (selection == 0) {
                                  if(cm.haveItem(4011020, 6)) {
                                  cm.gainItem(1022994, 1);
                  cm.gainItem(4011020, -6);
                                  cm.sendOk("Here's your #gMonster#k item!");
                  cm.dispose();
        }
        else {
        cm.sendOk("You don't have enough #gMonster#k coins! Come back when you have enough.");
        cm.dispose();
        }
                  } else if (selection == 1) {
                                  if(cm.haveItem(4011020, 6)) {
                                  cm.gainItem(1442259, 1);
                  cm.gainItem(4011020, -6);
                                  cm.sendOk("Here's your #gMonster#k item!");
                  cm.dispose();
        }
        else {
        cm.sendOk("You don't have enough #gMonster#k coins! Come back when you have enough.");
        cm.dispose();
        }

                  } else if (selection == 2) {
                                  if(cm.haveItem(4011020, 6)) {
                                  cm.gainItem(1082975, 1);
                  cm.gainItem(4011020, -6);
                                  cm.sendOk("Here's your #gMonster#k item!");
                  cm.dispose();
				  }
        else {
        cm.sendOk("You don't have enough #gMonster#k coins! Come back when you have enough.");
        cm.dispose();

}
 }
 }
 }
