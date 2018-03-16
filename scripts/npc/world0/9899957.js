var status;
var text = "I am the gate of #rOrichalcos#k. I bare the soul of the defeated Black Mage commander.";

function start() {
    status = -1;
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == 1)
        status++;
    else {
        cm.sendOk("#e#kOk, see you next time!");
        cm.dispose();
        return;
    }
        if (status == 12) {   
            cm.sendNext("I am the gate of #rOrichalcos#k. I bare the soul of the defeated Black Mage commander.");  
        }  
        else if (status == 0) {  
    cm.sendSimple("The gate of #rOrichalcos#k is told to bare the soul of a lost commander.\r\n#L0#Reincarnate #bBlack Mage's commander#k");
        } 
        else if (status == 1) {
        
        if (selection == 0) {  
     if (cm.getPlayer().getMap().getMonsterCount() == 0 && cm.haveItem(9895255, 1)) {
            cm.summonMob(9895255, 1);
            cm.gainItem(4011025, -1);
            cm.dispose();
        } else {
      cm.sendOk("You don't have the required item, or the boss is already summoned.");
      cm.dispose();
}
}
        }  
    } 
