var ITEM = 4011025; //4000000
var MOB = 9895257;  //100100

var MSG_1 = "I am the gate of #rOrichalcos#k. I bare the soul of the defeated Black Mage commander.";
var MSG_2 = "The gate of #rOrichalcos#k is told to bare the soul of a lost commander.\r\n#L0#Reincarnate #bBlack Mage's commander#k";
var MSG_NO_ITEM = "You don't have the required item, or the boss is already summoned.";
var MSG_BYE = "#e#kOk, see you next time!";

var MapleLifeFactory = Java.type('server.life.MapleLifeFactory');
var status = 0;

function start() {
    action(1, 0, 0);
}

function action(mode, type, selection) {
    status++;
    if(mode==-1){
        cm.sendOk(MSG_BYE);
        return cm.dispose();
    }
    
    if(status==1){
        return cm.sendNext(MSG_1); 
    }

    if(status==2){
        return cm.sendSimple(MSG_2);
    }

    if(status==3){
        if(selection==-1){
            cm.sendOk(MSG_BYE);
            return cm.dispose();
        }
        if (cm.getPlayer().getMap().countMonster(MOB) == 0 && cm.haveItem(ITEM, 1)) {
            var mob = MapleLifeFactory.getMonster(MOB);
            cm.getPlayer().getMap().spawnMonsterOnGroundBelow(mob,cm.getPlayer().getPosition());
            cm.gainItem(ITEM, -1);
            return cm.dispose();
        }
        cm.sendOk(MSG_NO_ITEM);
    }
    cm.dispose();
}