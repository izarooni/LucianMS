/*
 

 
 */
importPackage(Packages.tools);
var LifeFactory = Java.type("server.life.MapleLifeFactory");
var item1 = 4011019;
var item2 = 4011018;
var item3 = 4011017;
var item4 = 4011016;
var amount = 1;
var status = 0;

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == -1) {
        cm.dispose();
    } else {
        if (mode == 0) {
            cm.sendOk("...");
            cm.dispose();
            return;
        }
        status++;
        if (status == 0) {
            cm.sendNext("Good to see you again. As you can see, above us is a\r\n#d#eBlack Hole#n#k, \r\nYou need to find all 4 black holes in order for us to continue.");
        } else if (status == 1) {
            if (cm.haveItem(item1, amount) && cm.haveItem(item2, amount) && cm.haveItem(item3, amount) && cm.haveItem(item4, amount)) {
                cm.warp(43);
                cm.removeAll(4011019);
                cm.removeAll(4011018);
                cm.removeAll(4011017);
                cm.removeAll(4011016);
                cm.sendOk("This place does not seem so safe as it should..\r\n\r\nYou better check it out but be carefull. Good Luck!");
                cm.dispose();
            } else {
                cm.sendOk("You still need to collect black holes!");
                cm.dispose();
            }
            cm.dispose();
        }
    }
}