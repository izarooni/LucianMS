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
            cm.sendSimple ("Hello, there. Do you love #gMonster Energy#k? Trade me some leaves for my future experiments? You can use the coins at the #gVenem#k NPC, or trade them in by clicking the #bTrade#k button besides the cash button and buy some epic things! \r\n\ #L0# Trade 250 #i4000313# for 1 #i4011020# \r\n #L1# Trade 500 #i4001126# for 1 #i4011020#"); 
                  } else if (selection == 0) {
                                  if(cm.haveItem(4000313, 250)) {
                                  cm.gainItem(4011020, 1);
                  cm.gainItem(4000313, -250);
                                  cm.sendOk("Here's your #gMonster#k coin!");
                  cm.dispose();
        }
        else {
        cm.sendOk("You don't have enough #eGolden Leaves#k Come back when you have enough.");
        cm.dispose();
        }
                  } else if (selection == 1) {
                                  if(cm.haveItem(4001126, 500)) {
                                  cm.gainItem(4011020, 1);
                  cm.gainItem(4001126, -500);
                                  cm.sendOk("Here's your #gMonster#k coin!");
                  cm.dispose();
   

}
 }
 }
 }
