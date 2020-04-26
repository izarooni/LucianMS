//Monster Park Gear Trader NPC
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
            cm.sendSimple ("Are you enjoying Monster Park? If you are, then use the #i4310020# coins you've gathered from entering the park to purchase my goods. \r\n\ #L0# Trade 50 #i4310020# for 1 #i1012270# \r\n #L1# Trade 50 #i4310020# for 1 #i1122058# \r\n\ #L2# Trade 100 #i4310020# for 1 #i1122248# \r\n #L3# Trade 75 #i4310020# for 1 #i1132145# \r\n #L4# Trade 75 #i4310020# for 1 #i1102327# \r\n #L5# Trade 150 #i4310020# for 1 #i1102556# \r\n #L6# Trade 100 #i4310020# for 1#i1902545# \r\n #L7# Trade 75 #i4310020# for 1#i4011509# \r\n #L8# Trade 50 #i4310020# for 1#i4011510# \r\n #L9# Trade 50 #i4310020# for 1#i4011051#");
                  } else if (selection == 0) {
                                  if(cm.haveItem(4310020, 50)) {
                                  cm.gainItem(1012270, 1);
                  cm.gainItem(4310020, -50);
                                  cm.sendOk("Here's your item!");
                  cm.dispose();
        }
        else {
        cm.sendOk("You don't have enough Monster Park coins! Come back when you have enough.");
        cm.dispose();
        }
                  } else if (selection == 1) {
                                  if(cm.haveItem(4310020, 50)) {
                                  cm.gainItem(1122058, 1);
                  cm.gainItem(4310020, -50);
                                  cm.sendOk("Here's your #gMonster#k item!");
                  cm.dispose();
        }
        else {
        cm.sendOk("You don't have enough Monster Park coins! Come back when you have enough.");
        cm.dispose();
        }
 
                  } else if (selection == 2) {
                                  if(cm.haveItem(4310020, 100)) {
                                  cm.gainItem(1122248, 1);
                  cm.gainItem(4310020, -100);
                                  cm.sendOk("Here's your #gMonster#k item!");
                  cm.dispose();
                  }
        else {
        cm.sendOk("You don't have enough Monster Park coins! Come back when you have enough.");
        cm.dispose();
        }
 
                } else if (selection == 3) {
                                  if(cm.haveItem(4310020, 75)) {
                                  cm.gainItem(1132145, 1);
                  cm.gainItem(4310020, -75);
                                  cm.sendOk("Here's your item!");
                  cm.dispose();
        }
        else {
        cm.sendOk("You don't have enough Monster Park coins! Come back when you have enough.");
        cm.dispose();
        }
 
                } else if (selection == 4) {
                                  if(cm.haveItem(4310020, 75)) {
                                  cm.gainItem(1102327, 1);
                  cm.gainItem(4310020, -75);
                                  cm.sendOk("Here's your item!");
                  cm.dispose();
        }
        else {
        cm.sendOk("You don't have enough Monster Park coins! Come back when you have enough.");
        cm.dispose();
        }
 
                } else if (selection == 5) {
                                  if(cm.haveItem(4310020, 150)) {
                                  cm.gainItem(1102556, 1);
                  cm.gainItem(4310020, -150);
                                  cm.sendOk("Here's your item!");
                  cm.dispose();
        }
        else {
        cm.sendOk("You don't have enough Monster Park coins! Come back when you have enough.");
        cm.dispose();
        }
 
                } else if (selection == 6) {
                                  if(cm.haveItem(4310020, 100)) {
                                  cm.gainItem(1902545, 1);
                  cm.gainItem(4310020, -100);
                                  cm.sendOk("Here's your item!");
                  cm.dispose();
        }
        else {
        cm.sendOk("You don't have enough Monster Park coins! Come back when you have enough.");
        cm.dispose();
        }
		} else if (selection == 7) {
                                  if(cm.haveItem(4310020, 75)) {
                                  cm.gainItem(4011509, 1);
                  cm.gainItem(4310020, -75);
                                  cm.sendOk("Here's your item!");
                  cm.dispose();
        }
        else {
        cm.sendOk("You don't have enough Monster Park coins! Come back when you have enough.");
        cm.dispose();
        }
		} else if (selection == 8) {
                                  if(cm.haveItem(4310020, 50)) {
                                  cm.gainItem(4011510, 1);
                  cm.gainItem(4310020, -50);
                                  cm.sendOk("Here's your item!");
                  cm.dispose();
        }
        else {
        cm.sendOk("You don't have enough Monster Park coins! Come back when you have enough.");
        cm.dispose();
        }
		} else if (selection == 8) {
                                  if(cm.haveItem(4310020, 50)) {
                                  cm.gainItem(40, 1);
                  cm.gainItem(4310020, -50);
                                  cm.sendOk("Here's your item!");
                  cm.dispose();
        }
        else {
        cm.sendOk("You don't have enough Monster Park coins! Come back when you have enough.");
        cm.dispose();
        }
 }
 }
 }