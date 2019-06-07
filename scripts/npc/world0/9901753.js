//Eye Scanner NPC Trader

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
            cm.sendSimple ("Hello, there. I am the dealer of the Eye Scanners! \r\n\Are you interested in any of the eye Scanners? \r\nEye scanners increase the drop rate of Dragon Balls by 10%!#k \r\n\ #L0# Trade 6 #i4260002# for 1 #i1022999# \r\n #L1# Trade 6 #i4260002# for 1 #i1022995# \r\n\ #L2# Trade 6 #i4260002# for 1 #i1022996#"); 
                  } else if (selection == 0) {
                                  if(cm.haveItem(4260002, 6)) {
                                  cm.gainItem(1022999, 1);
                  cm.gainItem(4260002, -6);
                                  cm.sendOk("Here's your Red Eye Scanner!");
                  cm.dispose();
        }
        else {
        cm.sendOk("You don't have enough crystals! Come back when you have enough.");
        cm.dispose();
        }
                  } else if (selection == 1) {
                                  if(cm.haveItem(4260002, 6)) {
                                  cm.gainItem(1022995, 1);
                  cm.gainItem(4260002, -6);
                                  cm.sendOk("Here's your Green Eye Scanner!");
                  cm.dispose();
        }
        else {
        cm.sendOk("You don't have enough crystals! Come back when you have enough.");
        cm.dispose();
        }

                  } else if (selection == 2) {
                                  if(cm.haveItem(4260002, 6)) {
                                  cm.gainItem(1022996, 1);
                  cm.gainItem(4260002, -6);
                                  cm.sendOk("Here's your Pink Eye Scanner!");
                  cm.dispose();
        }
        else {
        cm.sendOk("You don't have enough crystals! Come back when you have enough.");
        cm.dispose();

}
 }
 }
 }
