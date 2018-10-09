
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
            cm.sendSimple ("Interested in mounts? \r\n\ #L0# Trade 5 #i4260002# for 1 #i1902025# \r\n #L1# Trade 5 #i4260002# for 1 #i1902024# \r\n\ #L2# Trade 5 #i4260002# for 1 #i1902027# \r\n\ #L3# Trade 6 #i4260002# for 1 #i1902028#"); 
                  } else if (selection == 0) {
                                  if(cm.haveItem(4260002, 5)) {
                                  cm.gainItem(1902025, 1);
                  cm.gainItem(4260002, -5);
                                  cm.sendOk("Here's your Blue Eye Scanner!");
                  cm.dispose();
        }
        else {
        cm.sendOk("You don't have enough crystals! Come back when you have enough.");
        cm.dispose();
        }
                  } else if (selection == 1) {
                                  if(cm.haveItem(4260002, 5)) {
                                  cm.gainItem(1902024, 1);
                  cm.gainItem(4260002, -5);
                                  cm.sendOk("Here's your Green Eye Scanner!");
                  cm.dispose();
        }
        else {
        cm.sendOk("You don't have enough crystals! Come back when you have enough.");
        cm.dispose();
        }

                  } else if (selection == 2) {
                                  if(cm.haveItem(4260002, 5)) {
                                  cm.gainItem(1902027, 1);
                  cm.gainItem(4260002, -5);
                                  cm.sendOk("Here's your Pink Eye Scanner!");
                  cm.dispose();
        }
        else {
        cm.sendOk("You don't have enough crystals! Come back when you have enough.");
        cm.dispose();
        }  } else if (selection == 3) {
                                  if(cm.haveItem(4260002, 5)) {
                                  cm.gainItem(1902028, 1);
                  cm.gainItem(4260002, -5);
                                  cm.sendOk("Here's your  Red Eye Scanner!");
                  cm.dispose();
        }
        else {
        cm.sendOk("You don't have enough crystals! Come back when you have enough.");
        cm.dispose();

}
 }
 }
 }
